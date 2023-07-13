package com.discoverable.discoverablesdk.client

import com.discoverable.discoverablesdk.DiscoverablePath
import com.discoverable.discoverablesdk.configuration.DiscoverableContext
import com.discoverable.discoverablesdk.headers
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableMessage
import com.discoverable.discoverablesdk.model.asString
import com.discoverable.discoverablesdk.model.toMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive

class DiscoverableWebSocket(
    private val client: HttpClient,
    private val discoverableContext: DiscoverableContext
) {

    private var socket: WebSocketSession? = null
    private var discoverableHost : Discoverable? = null

    suspend fun initSocket(discoverable: Discoverable) : Boolean {
        return try {
            socket = client.webSocketSession(DiscoverablePath.WebSocket) {
                url {
                    headers()
                    host = discoverable.ipAddress
                    port = discoverableContext.discoverablePort
                    method = HttpMethod.Get
                }
            }
            discoverableHost = discoverable
            socket?.isActive == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun URLBuilder.headers() = discoverableContext.discoverableIdentity.headers

    suspend fun send(message: DiscoverableMessage) : Boolean {
        if (socket == null) {
            return false
        }

        return try {
            socket?.outgoing?.send(Frame.Text(message.asString()))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun observeMessage() : Flow<DiscoverableMessage> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.map {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    json.toMessage()
                } ?: flow {  }
        } catch (e: Exception) {
            e.printStackTrace()
            flow {  }
        }
    }

    suspend fun closeSocket() {
        discoverableHost = null
        socket?.close(CloseReason(CloseReason.Codes.NORMAL, "Client terminated session"))
    }

    @Synchronized
    fun isConfigured() : Boolean {
        return socket != null && socket?.isActive == true
    }

    val host : Discoverable? = discoverableHost
}