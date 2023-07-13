package com.discoverable.discoverablesdk

import android.content.Intent
import android.os.Build
import com.discoverable.discoverablesdk.configuration.DiscoverableServiceConfiguration
import com.discoverable.discoverablesdk.exceptions.DiscoverableRuntimeException
import com.discoverable.discoverablesdk.model.Discoverable
import com.discoverable.discoverablesdk.server.DiscoverableService
import io.ktor.http.Headers
import io.ktor.http.headers
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

fun Intent?.getDiscoverableConfigurationOrThrow() : DiscoverableServiceConfiguration {
    val extras = this?.extras ?: throw DiscoverableRuntimeException(DiscoverableConstants.NO_DISCOVERABLE_CONFIGURATION_PRIVIDED)

    val configuration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        extras.getParcelable(DiscoverableService.CONFIGURATION_KEY, DiscoverableServiceConfiguration::class.java)
    } else {
        extras.getParcelable(DiscoverableService.CONFIGURATION_KEY)
    }

    return configuration ?: throw DiscoverableRuntimeException(DiscoverableConstants.NO_DISCOVERABLE_CONFIGURATION_PRIVIDED)
}

val DiscoverableService.discoverableApplication: DiscoverableApplication
    get() = application as DiscoverableApplication

val ApplicationCall.name: String
    get() = request.headers["name"] ?: throw DiscoverableRuntimeException("Headers not conformed")

val ApplicationCall.ip: String
    get() = request.headers["ip"] ?: throw DiscoverableRuntimeException("Headers not conformed")

fun AtomicInteger.generateIncrementalCacheKey(base: String) = if (get() > 0) "${base}${addAndGet(1)}" else "${base}${0}"

fun Int.generateIncrementalCacheKey(base: String) = "${base}${this}"

val Discoverable.headers: Headers
    get() = headers {
        append("name", name)
        append("ip", ipAddress)
    }

fun Int.writeToFile(file: File) {
    file.writeText(toString())
}

fun File.readInt() = readText().toInt()