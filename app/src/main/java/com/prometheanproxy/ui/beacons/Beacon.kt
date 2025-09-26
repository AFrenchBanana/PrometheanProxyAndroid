package com.prometheanproxy.ui.beacons

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Beacon(
    val userID: String,
    val uuid: String,
    val address: String,
    val hostname: String,
    val operatingSystem: String,
    val lastBeacon: String,
    val nextBeacon: String,
    val timer: String,
    val jitter: String
) : Parcelable