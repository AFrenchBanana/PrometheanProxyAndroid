package com.promethean.proxy.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.annotation.RequiresPermission
import com.promethean.proxy.login.isValidToken
import com.promethean.proxy.main.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.delay
import kotlin.math.log
import kotlin.math.pow

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
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
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

    public fun getJsonContentType(): String {
        return "application/json; charset=utf-8"
    }

    suspend fun postToServerWithToken(
        context: Context,
        uri: String,
        body: String,
        contentType: String,
        ): String {
        var token = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("token", "")
        var tokenExpiry = context.getSharedPreferences("Settings", Context.MODE_PRIVATE).getString("tokenExpiry", "")

        if (!tokenExpiry.isNullOrEmpty() && !token.isNullOrEmpty() && tokenExpiry.isValidToken()) {
            Log.d("postWithToken", "Valid token continuing")
            return postToServer(uri, body, contentType, token)
        } else {
            return "Token Expired need to re-auth"
        }
    }


    suspend fun postToServer(
        uri: String,
        body: String,
        contentType: String,
        token : String? = ""
    ): String {
        val formattedUri = if (uri.startsWith("/")) uri else "/$uri"
        val url = fullUrl + formattedUri
        val mediaType = contentType.toMediaType()
        val requestBody = body.toRequestBody(mediaType)


        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", contentType)

        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()

        var lastException: Exception? = null
        var maxRetries = 3

        for (currentAttempt in 1..maxRetries) {
            try {
                return withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            if (response.code in 400..499) {
                                Log.e("Network Manager", "Client Error: ${response.code} for $url")
                                return@withContext ""
                            }
                            throw IOException("Server Error: ${response.code}")
                        }

                        val text = response.body?.string() ?: ""
                        Log.d("Network Manager", "Success from $url")
                        text
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.w("Network Manager", "Attempt $currentAttempt failed for $url: ${e.message}")

                if (currentAttempt < maxRetries && isRetryable(e)) {
                    val backoffMillis = (2.0.pow(currentAttempt.toDouble()) * 500).toLong()
                    delay(backoffMillis)
                } else {
                    break
                }
            }
        }

        Log.e("Network Manager", "Final failure for $url after $maxRetries attempts: ${lastException?.message}")
        return ""
    }

    private fun isRetryable(e: Exception): Boolean {
        return e is SocketTimeoutException ||
                e is IOException
    }
}