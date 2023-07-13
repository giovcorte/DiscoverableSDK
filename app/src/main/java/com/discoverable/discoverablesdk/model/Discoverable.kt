package com.discoverable.discoverablesdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Discoverable(val ipAddress: String, val name: String)

fun Discoverable.writeToFile(file: File) {
    file.writeText(Json.encodeToString(this))
}

fun File.readDiscoverable() : Discoverable = Json.decodeFromString(readText())