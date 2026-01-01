package com.promethean.proxy.network

import android.Manifest
import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.promethean.proxy.di.PreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class NetworkManager @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val connectivityManager: ConnectivityManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val URL_KEY = stringPreferencesKey("url")
        val PORT_KEY = intPreferencesKey("port")
        val TOKEN_KEY = stringPreferencesKey("token")
    }

    var fullUrl = ""

    init {
        scope.launch {
            updateConfig()
        }
    }

    suspend fun updateConfig() {

        val baseUrl = preferenceRepository.getUrl()
        val port = preferenceRepository.getPort()


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

    suspend fun getFromServer(uri: String, token: String? = null): String = withContext(Dispatchers.IO) {
        if (fullUrl.isEmpty()) return@withContext ""

        val url = fullUrl + (if (uri.startsWith("/")) uri else "/$uri")
        Log.d("NetworkManager", "GET Request URL: $url") // Added logging for URI

        try {
            val request = Request.Builder()
                .url(url)
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("NetworkManager", "GET Response Body: $responseBody") // Added logging for Body
                responseBody
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "GET Request failed for $url", e)
            ""
        }
    }

    suspend fun getWithToken(uri: String): String {
        val token = preferenceRepository.getToken()
        if (token.isEmpty()) return "Expired"
        return getFromServer(uri, token)
    }

    suspend fun postWithToken(uri: String, body: String): String {
        val token = preferenceRepository.getToken()
        if (token.isEmpty()) return "Expired"
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
        val data = getFromServer(
            uri = "/ping",
        )
        return data.contains("pong")
    }

    fun getJsonContentType(): String = "application/json; charset=utf-8"
}
