package com.discoverable.discoverablesdk

import com.discoverable.discoverablesdk.client.DiscoverableClient
import com.discoverable.discoverablesdk.model.Discoverable
import java.io.File

@Suppress("unused")
class DiscoverableRepository(
    discoverableApplication: DiscoverableApplication,

) {
    /**
     * Client
     */
    private val discoverableClient = DiscoverableClient(discoverableApplication)

    /**
     * Http
     */

    private val discoverableIncomingHttpRequest = discoverableApplication.discoverableIncomingFlow

    suspend fun availableDiscoverables() : List<Discoverable> {
        return discoverableClient.discoverHttpServers()
    }

    fun cachedDiscoverables() : List<Discoverable> {
        return discoverableClient.cachedHttpServers()
    }

    suspend fun availableCachedDiscoverables() :List<Discoverable> {
        return discoverableClient.cachedHttpServers().filter { discoverableClient.isHttpServer(it.ipAddress) != null }
    }

    suspend fun sendHttpMessage(message: String, discoverable: Discoverable) : Boolean {
        return discoverableClient.postOnHttp(message, discoverable)
    }

    suspend fun sendHttpBroadcast(message: String) {
        discoverableClient.broadcastOnHttp(message)
    }

    suspend fun sendHttpFile(file: File, discoverable: Discoverable) : Boolean {
        return discoverableClient.uploadOnHttp(file, discoverable)
    }

    /**
     * WebSocket
     */

    val incomingWebSocket = discoverableClient.observeFromWebSocket()

    suspend fun connectWebSocket(sameOf: Discoverable) : Boolean {
        val discoverableServer = discoverableClient.getWebSocketHost(sameOf) ?: return false
        return discoverableClient.connectWebSocket(discoverableServer)
    }

    suspend fun sendOnWebSocket(message: String, discoverable: Discoverable) : Boolean {
        return discoverableClient.sendOnWebSocket(message, discoverable)
    }

    suspend fun closeWebSocket() {
        discoverableClient.closeWebSocket()
    }

    fun isWebSocketConfigured() : Boolean {
        synchronized(discoverableClient) {
            return discoverableClient.isWebSocketConnected()
        }
    }

    fun webSocketConfiguration() = discoverableClient.webSocketHost()
}