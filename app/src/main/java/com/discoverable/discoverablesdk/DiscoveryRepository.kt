package com.discoverable.discoverablesdk

import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableMessage
import com.discoverable.discoverablesdk.model.DiscoverableResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DiscoverableRepository {

    val discoverableIncomingHttpRequest : Flow<DiscoverableResult>

    val incomingWebSocket: Flow<DiscoverableMessage>

    suspend fun availableDiscoverables() : List<Discoverable>

    fun cachedDiscoverables() : List<Discoverable>

    suspend fun availableCachedDiscoverables() : List<Discoverable>

    suspend fun sendHttpMessage(message: String, discoverable: Discoverable) : Boolean

    suspend fun sendHttpBroadcast(message: String)

    suspend fun sendHttpFile(file: File, discoverable: Discoverable) : Boolean

    suspend fun connectWebSocket(sameOf: Discoverable) : Boolean

    suspend fun sendOnWebSocket(message: String, discoverable: Discoverable) : Boolean

    suspend fun closeWebSocket()

    fun isWebSocketConfigured() : Boolean

    fun webSocketConfiguration(): Discoverable?
}