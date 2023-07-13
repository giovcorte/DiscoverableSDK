package com.discoverable.discoverablesdk.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.discoverable.discoverablesdk.DiscoverablePath
import com.discoverable.discoverablesdk.DiscoverableRepository
import com.discoverable.discoverablesdk.cache.DiskCache
import com.discoverable.discoverablesdk.cache.Utils.formatKey
import com.discoverable.discoverablesdk.configuration.DiscoverableContext
import com.discoverable.discoverablesdk.configuration.DiscoverableServiceConfiguration
import com.discoverable.discoverablesdk.discoverableApplication
import com.discoverable.discoverablesdk.exceptions.DiscoverableRuntimeException
import com.discoverable.discoverablesdk.generateIncrementalCacheKey
import com.discoverable.discoverablesdk.ip
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableMessage
import com.discoverable.discoverablesdk.model.DiscoverableResult
import com.discoverable.discoverablesdk.model.toMessage
import com.discoverable.discoverablesdk.name
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

abstract class DiscoverableService : Service() {

    companion object {
        const val CONFIGURATION_KEY = "discoverable-configuration"
    }

    private val discoverableIncrementalFileCount = AtomicInteger(0)

    @OptIn(DelicateCoroutinesApi::class)
    private val coroutineScope = GlobalScope
    private var job: Job? = null

    private lateinit var discoverableContext: DiscoverableContext
    private lateinit var discoverableRepository: DiscoverableRepository
    private lateinit var configuration: DiscoverableServiceConfiguration
    private lateinit var diskCache: DiskCache
    private val discoverableClientManager = DiscoverableClientManager()

    abstract suspend fun onDiscoverableTextReceived(result: DiscoverableResult)
    abstract suspend fun onDiscoverableFileReceived(result: DiscoverableResult)

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onConfigureService()
        onInitializeService(intent)

        super.onStartCommand(intent, flags, startId)

        job = coroutineScope.launch {
            embeddedServer(Netty, port = configuration.Port) {
                install(ContentNegotiation) {
                    json()
                }

                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(Json)
                }

                install(Routing) {
                    get(DiscoverablePath.Identity) {
                       call.respond(HttpStatusCode.OK, discoverableContext.discoverableIdentity)
                    }

                    post(DiscoverablePath.Text) {
                        val result = DiscoverableResult.DiscoverableText(Discoverable(call.ip, call.name), call.receive())
                        onDiscoverableTextReceived(result)
                        discoverableContext.sendDiscoverableResult(result)
                        call.respond(HttpStatusCode.OK)
                    }

                    post(DiscoverablePath.File) {
                        val cacheKey = discoverableIncrementalFileCount.generateIncrementalCacheKey(call.name.formatKey())
                        diskCache.edit(cacheKey)?.apply {
                            call.receiveChannel().copyAndClose(file().writeChannel())
                            commit()
                            val result = DiscoverableResult.DiscoverableFile(Discoverable(call.ip, call.name), cacheKey)
                            onDiscoverableFileReceived(result)
                            discoverableContext.sendDiscoverableResult(result)
                        }
                        call.respond(HttpStatusCode.OK)
                    }

                    get(DiscoverablePath.WebSocketConfig) {
                        val isWebSocketConnected = discoverableRepository.isWebSocketConfigured()
                        if (isWebSocketConnected) {
                            val host = discoverableRepository.webSocketConfiguration()
                            if (host != null) {
                                call.respond(host)
                            } else {
                                call.respond(HttpStatusCode.InternalServerError)
                            }
                        } else {
                            val discoverableServer = discoverableRepository.availableCachedDiscoverables().random()
                            if (discoverableRepository.connectWebSocket(discoverableServer)) {
                                call.respond(HttpStatusCode.OK, discoverableServer)
                            } else {
                                call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                    }

                    webSocket(DiscoverablePath.WebSocket) {
                        try {
                            val id = call.ip
                            val thisConnection = discoverableClientManager.cleanAndCreateConnection(this, id)
                            discoverableClientManager.connections += thisConnection

                            incoming.consumeEach { frame ->
                                if (frame is Frame.Text) {
                                    val receivedText = frame.readText()
                                    val discoverableMessage: DiscoverableMessage = receivedText.toMessage()
                                    if (discoverableMessage.destination == null) {
                                        discoverableClientManager.sendMessageToAll(discoverableMessage)
                                    } else {
                                        discoverableClientManager.sendMessageTo(discoverableMessage)
                                    }
                                }
                            }
                        } catch (e: DiscoverableRuntimeException) {
                            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid headers"))
                        }
                    }
                }
            }.start(wait = true)
        }

        return START_STICKY
    }

    private fun onConfigureService() {
        discoverableContext = discoverableApplication.discoverableContext
        discoverableRepository = discoverableApplication.discoverableRepository
        configuration = discoverableContext.discoverableServiceConfiguration
        diskCache = discoverableContext.discoverableDiskCache
    }

    open fun onInitializeService(intent: Intent?) {
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                this,
                0, Intent(this, discoverableContext.discoverableActivity.java), PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(configuration.NotificationChannelId, configuration.NotificationChannelName, NotificationManager.IMPORTANCE_MIN).apply {
                description = configuration.NotificationChannelDescription
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

            val notification: Notification = Notification.Builder(this, configuration.NotificationChannelId)
                .setOngoing(true)
                .setContentTitle(configuration.NotificationChannelName)
                .setSmallIcon(configuration.NotificationIcon)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(configuration.NotificationOngoingId, notification)
        } else {
            val notification: Notification = NotificationCompat.Builder(this, configuration.NotificationChannelId)
                .setOngoing(true)
                .setContentTitle(configuration.NotificationChannelName)
                .setSmallIcon(configuration.NotificationIcon)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(configuration.NotificationOngoingId, notification)
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null
}