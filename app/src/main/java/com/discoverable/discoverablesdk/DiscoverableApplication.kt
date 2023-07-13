package com.discoverable.discoverablesdk

import android.app.Application
import android.content.Intent
import com.discoverable.discoverablesdk.cache.DiskCache
import com.discoverable.discoverablesdk.configuration.DiscoverableContext
import com.discoverable.discoverablesdk.client.DiscoverableClient
import com.discoverable.discoverablesdk.model.DiscoverableResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Suppress("unused")
abstract class DiscoverableApplication : Application() {

    companion object {
       internal var isDiscoverableServiceRunning = false
    }

    abstract val discoverableContext: DiscoverableContext

    private val discoverableIncomingChannel = Channel<DiscoverableResult>()
    val discoverableIncomingFlow : Flow<DiscoverableResult>
        get() = discoverableIncomingChannel.receiveAsFlow()

    private lateinit var discoverableServiceIntent: Intent
    lateinit var discoverableRepository: DiscoverableRepository

    override fun onCreate() {
        super.onCreate()
        discoverableServiceIntent = Intent(this, discoverableContext.discoverableService::class.java)
        discoverableRepository = DiscoverableRepository(this)
    }

    @Synchronized
    fun startDiscoverableService() {
        isDiscoverableServiceRunning = true
        startService(discoverableServiceIntent)
    }

    @Synchronized
    fun stopDiscoverableService() {
        stopService(discoverableServiceIntent)
        isDiscoverableServiceRunning = false
    }

    fun isDiscoverableServiceRunning() : Boolean = isDiscoverableServiceRunning

    suspend fun sendDiscoverableResult(data: DiscoverableResult) {
        discoverableIncomingChannel.send(data)
    }

    /*
    fun isDiscoverableServiceRunning() : Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (DiscoverableService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
     */

}