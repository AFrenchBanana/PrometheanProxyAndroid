package com.promethean.proxy.network

import android.Manifest
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.annotation.RequiresPermission
import com.promethean.proxy.di.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @SettingsPrefs private val prefs: SharedPreferences,
    private val connectivityManager: ConnectivityManager,
    private val okHttpClient: OkHttpClient
) {
    private var fullUrl: String = ""

    init {
        updateConfig()
    }

    fun updateConfig() {
        val baseUrl = prefs.getString("url", "")
        
        // Handle case where port might be stored as String (common in some preference libs)
        val port = try {
            prefs.getInt("port", 443)
        } catch (e: Exception) {
            prefs.getString("port", "443")?.toIntOrNull() ?: 443
        }

        if (!baseUrl.isNullOrEmpty()) {
            fullUrl = when {
                baseUrl.contains("://") -> baseUrl
                port == 80 -> "http://$baseUrl"
                port == 443 -> "https://$baseUrl"
                else -> "https://$baseUrl:$port"
            }
        } else {
            fullUrl = ""
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun haveNetwork(): Boolean = connectivityManager.activeNetwork != null

    suspend fun getFromServer(uri: String): String = withContext(Dispatchers.IO) {
        if (fullUrl.isEmpty()) return@withContext ""
        
        val url = fullUrl + (if (uri.startsWith("/")) uri else "/$uri")
        try {
            val request = Request.Builder()
                .url(url)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun postWithToken(uri: String, body: String): String {
        val token = prefs.getString("token", "")
        if (token.isNullOrEmpty()) return "Expired"
        return post(uri, body, "application/json", token)
    }

    suspend fun post(
        uri: String,
        body: String,
        contentType: String,
        token: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (fullUrl.isEmpty()) return@withContext ""

        val url = fullUrl + (if (uri.startsWith("/")) uri else "/$uri")
        try {
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(contentType.toMediaType()))
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun testConnection(): Boolean {
        if (fullUrl.isEmpty()) return false
        val data = getFromServer("/ping")
        return data.contains("pong")
    }

    fun getJsonContentType(): String = "application/json; charset=utf-8"
}
