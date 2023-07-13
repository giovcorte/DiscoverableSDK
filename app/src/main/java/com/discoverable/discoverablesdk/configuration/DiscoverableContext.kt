package com.discoverable.discoverablesdk.configuration

import android.app.Activity
import com.discoverable.discoverablesdk.server.DiscoverableService
import com.discoverable.discoverablesdk.cache.DiskCache
import com.discoverable.discoverablesdk.model.Discoverable
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
    val discoverableServiceConfiguration: DiscoverableServiceConfiguration
) {
    val discoverableDiskCache = DiskCache(discoverableCacheFolder, discoverableCacheFolderSize, discoverableAppVersion)
}