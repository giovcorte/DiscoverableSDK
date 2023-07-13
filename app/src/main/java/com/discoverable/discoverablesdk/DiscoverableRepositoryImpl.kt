package com.discoverable.discoverablesdk

import com.discoverable.discoverablesdk.client.DiscoverableClient
import com.discoverable.discoverablesdk.configuration.DiscoverableContext
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableResult
import kotlinx.coroutines.flow.Flow
import java.io.File

@Suppress("unused")
class DiscoverableRepositoryImpl(
    discoverableContext: DiscoverableContext,
) : DiscoverableRepository {
    private val discoverableClient = DiscoverableClient(discoverableContext)

    /**
     * Http
     */

    override val discoverableIncomingHttpRequest : Flow<DiscoverableResult> = discoverableContext.discoverableIncomingHttpFlow

    override suspend fun availableDiscoverables() : List<Discoverable> {
        return discoverableClient.discoverHttpServers()
    }

    override fun cachedDiscoverables() : List<Discoverable> {
        return discoverableClient.cachedHttpServers()
    }

    override suspend fun availableCachedDiscoverables() :List<Discoverable> {
        return discoverableClient.cachedHttpServers().filter { discoverableClient.isHttpServer(it.ipAddress) != null }
    }

    override suspend fun sendHttpMessage(message: String, discoverable: Discoverable) : Boolean {
        return discoverableClient.postOnHttp(message, discoverable)
    }

    override suspend fun sendHttpBroadcast(message: String) {
        discoverableClient.broadcastOnHttp(message)
    }

    override suspend fun sendHttpFile(file: File, discoverable: Discoverable) : Boolean {
        return discoverableClient.uploadOnHttp(file, discoverable)
    }

    /**
     * WebSocket
     */

    override val incomingWebSocket = discoverableClient.observeFromWebSocket()

    override suspend fun connectWebSocket(sameOf: Discoverable) : Boolean {
        val discoverableServer = discoverableClient.getWebSocketHost(sameOf) ?: return false
        return discoverableClient.connectWebSocket(discoverableServer)
    }

    override suspend fun sendOnWebSocket(message: String, discoverable: Discoverable) : Boolean {
        return discoverableClient.sendOnWebSocket(message, discoverable)
    }

    override suspend fun closeWebSocket() {
        discoverableClient.closeWebSocket()
    }

    override fun isWebSocketConfigured() : Boolean {
        synchronized(discoverableClient) {
            return discoverableClient.isWebSocketConnected()
        }
    }

    override fun webSocketConfiguration() = discoverableClient.webSocketHost()
}