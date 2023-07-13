package com.discoverable.discoverablesdk.configuration

import android.app.Activity
import android.net.wifi.WifiManager
import com.discoverable.discoverablesdk.server.DiscoverableService
import com.discoverable.discoverablesdk.cache.DiskCache
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.model.DiscoverableResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import kotlin.reflect.KClass

class DiscoverableContext(
    discoverableCacheFolder: File,
    discoverableCacheFolderSize: Long,
    discoverableAppVersion : Int,
    val discoverablePort: Int,
    val discoverableActivity: KClass<in Activity>,
    val discoverableService: KClass<in DiscoverableService>,
    val discoverableIdentity: Discoverable,
    val discoverableServiceConfiguration: DiscoverableServiceConfiguration,
    val wifiManager: WifiManager,
    private val discoverableIncomingChannel: Channel<DiscoverableResult> = Channel()
) {
    val discoverableDiskCache = DiskCache(discoverableCacheFolder, discoverableCacheFolderSize, discoverableAppVersion)

    val discoverableIncomingHttpFlow = discoverableIncomingChannel.receiveAsFlow()
    suspend fun sendDiscoverableResult(data: DiscoverableResult) {
        discoverableIncomingChannel.send(data)
    }

}