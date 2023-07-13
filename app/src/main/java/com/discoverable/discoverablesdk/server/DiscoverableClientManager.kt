package com.discoverable.discoverablesdk.server

import com.discoverable.discoverablesdk.model.DiscoverableMessage
import com.discoverable.discoverablesdk.model.asString
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.isActive
import java.util.Collections

class DiscoverableClientManager {
    val connections : MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())

    suspend fun cleanAndCreateConnection(session: DefaultWebSocketSession, id: String) : Connection {
        val thisConnection = Connection(session, id)
        try {
            val deleteConnection = mutableListOf<Connection>()
            connections.forEach {
                if (!it.session.isActive) {
                    it.session.close(CloseReason(CloseReason.Codes.NORMAL, "Inactive"))
                    deleteConnection.add(it)
                }
            }
            deleteConnection.forEach {
                connections.remove(it)
            }
        } catch (ignored: Exception) {

        }
        return thisConnection
    }

    suspend fun sendMessageTo(message: DiscoverableMessage) {
        val deleteConnection = mutableListOf<Connection>()
        connections.forEach {
            try {
                if (it.id == message.destination?.ipAddress) {
                    it.session.send(Frame.Text(message.asString()))
                }
            } catch (ignored: Exception) {
                deleteConnection.add(it)
            }
        }
        deleteConnection.forEach {
            it.session.close(CloseReason(CloseReason.Codes.NORMAL, "Inactive"))
            connections.remove(it)
        }
    }

    suspend fun sendMessageToAll(message: DiscoverableMessage) {
        val deleteConnection = mutableListOf<Connection>()
        connections.forEach {
            try {
                it.session.send(Frame.Text(message.asString()))
            } catch (ignored: Exception) {
                deleteConnection.add(it)
            }
        }
        deleteConnection.forEach {
            it.session.close(CloseReason(CloseReason.Codes.NORMAL, "Inactive"))
            connections.remove(it)
        }
    }
}