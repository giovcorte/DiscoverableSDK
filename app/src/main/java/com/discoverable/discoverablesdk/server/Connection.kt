package com.discoverable.discoverablesdk.server

import io.ktor.websocket.DefaultWebSocketSession
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session: DefaultWebSocketSession, val id: String) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val name = "$id${lastId.getAndIncrement()}"
}