package com.discoverable.discoverablesdk.cache

object Utils {

    fun String.formatKey(): String {
        val formatted = this.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        return formatted.substring(0, if (formatted.length >= 120) 119 else formatted.length)
    }

}