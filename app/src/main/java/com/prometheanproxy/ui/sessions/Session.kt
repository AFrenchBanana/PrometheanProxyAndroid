package com.prometheanproxy.ui.sessions

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Session(
    val userID: String,
    val uuid: String,
    val address: String,
    val hostname: String,
    val operatingSystem: String,
    val lastSession: String, // Renamed from lastBeacon
    val nextSession: String, // Renamed from nextBeacon
    val timer: String,
    val jitter: String
) : Parcelable