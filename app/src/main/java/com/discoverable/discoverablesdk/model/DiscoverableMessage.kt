package com.discoverable.discoverablesdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DiscoverableMessage(
    val sender: Discoverable,
    val destination: Discoverable?,
    val message: String
)

fun DiscoverableMessage.asString() = Json.encodeToString(this)

fun String.toMessage() = Json.decodeFromString<DiscoverableMessage>(this)

