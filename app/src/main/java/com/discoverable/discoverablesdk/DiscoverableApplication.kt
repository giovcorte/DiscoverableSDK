package com.discoverable.discoverablesdk

import android.app.Application
import android.content.Intent
import com.discoverable.discoverablesdk.configuration.DiscoverableContext

@Suppress("unused")
abstract class DiscoverableApplication : Application() {

    companion object {
       internal var isDiscoverableServiceRunning = false
    }

    abstract val discoverableContext: DiscoverableContext

    private lateinit var discoverableServiceIntent: Intent
    lateinit var discoverableRepository: DiscoverableRepository

    override fun onCreate() {
        super.onCreate()
        discoverableServiceIntent = Intent(this, discoverableContext.discoverableService::class.java)
        discoverableRepository = DiscoverableRepository(discoverableContext)
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

}