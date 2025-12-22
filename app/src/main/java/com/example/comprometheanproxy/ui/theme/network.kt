package com.example.proxy.networkConfig

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.annotation.RequiresPermission
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class NetworkManager(context: Context) {


    var connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var baseUrl: String? = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("IP", "")
    var port: String? = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("Port", "")
    var network : Network? = null
    private val fullUrl: String
    var okHttpClient = OkHttpClient()




    init {
        if (baseUrl.isNullOrEmpty() || port.isNullOrEmpty()) {
            Log.e("Network Manager", "URL or Port is null or empty")
            throw IllegalArgumentException("URL or Port is null or empty")
        }

        fullUrl = if (port == "80" || baseUrl!!.contains(":")) {
            baseUrl!!
        } else {
            "$baseUrl:$port"
        }

        if (haveNetwork()) {
            Log.d("Network Manager", "Network available on initialization")
        }
    }

    fun login() {

    }


    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun haveNetwork(): Boolean {
        network = connectivityManager.activeNetwork
        if (network == null) {
            Log.e("Network Manager", "No network connection")
            return false
        }
        return true
    }

    fun canTalkToServer(): Boolean {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = "".toRequestBody(mediaType)

        val request = Request.Builder()
            .url(fullUrl)
            .post(body)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            Log.e("Network Manager", "Failed to reach server: ${e.message}")
            false
        }
    }


}