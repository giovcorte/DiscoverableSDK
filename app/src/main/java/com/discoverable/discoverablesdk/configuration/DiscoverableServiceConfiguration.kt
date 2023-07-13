package com.discoverable.discoverablesdk.configuration

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class DiscoverableServiceConfiguration(
    val Port : Int,
    val NotificationOngoingId : Int,
    val NotificationChannelId : String,
    val NotificationChannelName : String,
    val NotificationChannelDescription : String,
    val NotificationIcon : Int
) : Parcelable