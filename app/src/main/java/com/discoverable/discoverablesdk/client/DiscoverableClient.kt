package com.discoverable.discoverablesdk.client

import com.discoverable.discoverablesdk.DiscoverablePath
import com.discoverable.discoverablesdk.configuration.DiscoverableContext
import com.discoverable.discoverablesdk.generateIncrementalCacheKey
import com.discoverable.discoverablesdk.headers
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableMessage
import com.discoverable.discoverablesdk.model.readDiscoverable
import com.discoverable.discoverablesdk.model.writeToFile
import com.discoverable.discoverablesdk.readInt
import com.discoverable.discoverablesdk.writeToFile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.readChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DiscoverableClient(discoverableContext: DiscoverableContext) {

    companion object {
        const val DISCOVERABLE_COUNT_KEY = "discoverable-count-key"
        const val DISCOVERABLE_BASE_KEY = "discoverable-base-key"
    }

    private val wifiManager = discoverableContext.wifiManager
    private val discoverablePort = discoverableContext.discoverablePort
    private val discoverableIdentity = discoverableContext.discoverableIdentity
    private val discoverableCache = discoverableContext.discoverableDiskCache

    private val discoverableCount = AtomicInteger(0)

    @get:Synchronized
    @set:Synchronized
    private var initialized = false

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    private val discoverableWebSocket = DiscoverableWebSocket(client, discoverableContext)
    private val discoverables = mutableSetOf<Discoverable>()

    private fun initialize() {
        if (initialized) return

        discoverableCache.get(DISCOVERABLE_COUNT_KEY)?.let { snapshot ->
            discoverableCount.set(snapshot.file().readInt())
            snapshot.close()
        } ?: run {
            val snapshot = discoverableCache.edit(DISCOVERABLE_COUNT_KEY)?.apply {
                discoverableCount.get().writeToFile(file())
            }?.commitAndGet()
            discoverableCount.set(snapshot?.file()?.readInt() ?: 0)
            snapshot?.close()
        }
        initialized = true
    }

    suspend fun isHttpServer(ipAddress: String) : Discoverable? {
        return try {
            client.get(DiscoverablePath.Identity) {
                url {
                    method = HttpMethod.Get
                    host = ipAddress
                    port = discoverablePort
                }
            }.body<Discoverable>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cachedHttpServers() : List<Discoverable> {
        initialize()

        for (i in 0..discoverableCount.get()) {
            discoverableCache.get(i.generateIncrementalCacheKey(DISCOVERABLE_BASE_KEY))?.apply {
                discoverables.add(file().readDiscoverable())
                close()
            }
        }
        return discoverables.toList()
    }

    suspend fun discoverHttpServers() : List<Discoverable> {
        initialize()
        try {
            val subnet = getSubnetAddress(wifiManager.dhcpInfo.gateway)
            for (i in 0..254) {
                val host = "$subnet.$i"
                isHttpServer(host)?.let { discoverable ->
                    discoverables.add(discoverable)
                    val cacheKey = discoverableCount.generateIncrementalCacheKey(DISCOVERABLE_BASE_KEY)
                    discoverableCache.edit(cacheKey)?.let { editor ->
                        discoverable.writeToFile(editor.file())
                        editor.commit()
                    }
                    discoverableCache.edit(DISCOVERABLE_COUNT_KEY)?.let { editor ->
                        discoverableCount.get().writeToFile(editor.file())
                        editor.commit()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return discoverables.toList()
    }

    suspend fun broadcastOnHttp(data: String) {
        discoverables.forEach {
            postOnHttp(data, it)
        }
    }

    suspend fun postOnHttp(data: String, discoverableTo: Discoverable) : Boolean {
        initialize()

        return try {
            val status = client.post(DiscoverablePath.Text) {
                headers()
                contentType(ContentType.Application.Json)
                method = HttpMethod.Post
                host = discoverableTo.ipAddress
                port = discoverablePort
                setBody(data)
            }.status
            status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadOnHttp(discoverableFile: File, discoverableTo: Discoverable) : Boolean {
        initialize()

        return try {
            val status = client.post(DiscoverablePath.File) {
                contentType(ContentType.Application.OctetStream)
                headers()
                method = HttpMethod.Post
                host = discoverableTo.ipAddress
                port = discoverablePort
                setBody(discoverableFile.readChannel())
            }.status
            status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getSubnetAddress(address: Int): String {
        return String.format(
            "%d.%d.%d",
            address and 0xff,
            address shr 8 and 0xff,
            address shr 16 and 0xff
        )
    }

    fun HttpRequestBuilder.headers() = discoverableIdentity.headers

    /**
     * WebSocket Methods
     */

    suspend fun getWebSocketHost(forDiscoverable: Discoverable) : Discoverable? {
        return try {
            client.get(DiscoverablePath.WebSocketConfig) {
                url {
                    method = HttpMethod.Get
                    host = forDiscoverable.ipAddress
                    port = discoverablePort
                }
            }.body<Discoverable>()
        } catch (e: Exception) {
            null
        }
    }

    fun isWebSocketConnected(): Boolean {
        return discoverableWebSocket.isConfigured()
    }

    suspend fun connectWebSocket(host: Discoverable): Boolean {
        return discoverableWebSocket.initSocket(host)
    }

    suspend fun sendOnWebSocket(message: String, discoverableTo: Discoverable) : Boolean {
        val discoverableMessage = DiscoverableMessage(discoverableIdentity, discoverableTo, message)
        return discoverableWebSocket.send(discoverableMessage)
    }

    fun observeFromWebSocket() : Flow<DiscoverableMessage> {
        return discoverableWebSocket.observeMessage()
    }

    suspend fun closeWebSocket() {
        discoverableWebSocket.closeSocket()
    }

    fun webSocketHost() = discoverableWebSocket.host
}