package com.discoverable.discoverablesdk.model

@Suppress("unused")
sealed class DiscoverableResult(val discoverableFrom: Discoverable) {
    class DiscoverableText(
        sender: Discoverable,
        val text: String
    ) : DiscoverableResult(sender)

    class DiscoverableFile(
        sender: Discoverable,
        val fileKey: String
    ) : DiscoverableResult(sender)
}