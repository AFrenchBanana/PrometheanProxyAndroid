package com.promethean.proxy.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NetworkManager(context: Context) {


    var connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var baseUrl: String? = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("url", "")
    var port: String? = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("port", "")
    var network : Network? = null
    private val fullUrl: String
    var okHttpClient = getUnsafeOkHttpClient()




    init {
        if (baseUrl.isNullOrEmpty() || port.isNullOrEmpty()) {
            Log.e("Network Manager", "URL or Port is null or empty")
            throw IllegalArgumentException("URL or Port is null or empty")
        }

        fullUrl = if (port == "80" || baseUrl!!.contains(":")) {
            baseUrl!!
        } else {
            "https://$baseUrl:$port"
        }

        if (haveNetwork()) {
            Log.d("Network Manager", "Network available on initialization")
        }
    }

    fun login() {

    }

    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
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


    suspend fun getFromServer(uri: String): String {
        Log.d("NetworkManager", "Getting $uri")
        val formattedUri = if (uri.startsWith("/")) uri else "/$uri"
        val url = fullUrl + formattedUri
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("Network Manager", "Unexpected code $response")
                        return@withContext ""
                    }

                    val text = response.body?.string() ?: ""
                    Log.d("Network Manager", "Response from $url: $text")
                    text
                }
            } catch (e: IOException) {
                Log.e("Network Manager", "Failed to reach server: ${e.message}")
                ""
            }
        }
    }


}