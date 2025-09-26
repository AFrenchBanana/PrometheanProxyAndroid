package com.prometheanproxy.connectivity
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.net.Socket
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object Connectivity {

    var authToken: String = ""
    private var socket: Socket? = null


    /**
     * WARNING: This is an insecure OkHttpClient builder.
     * Depends if the server wants to use a legit HTTP client.
     */
    fun getInsecureOkHttpClient(): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true } // Ignore hostname verification

            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Connects to a server and attempts to log in.
     *
     * This method must be called from a background thread.
     *
     * @param url The server address (e.g., "192.168.68.61:2001").
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @return A `Pair` where the first element is `true` on success and the second
     *         element is the server response body or an error message.
     */
    suspend fun connectServer(url: String, username: String, password: String): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = "https://$url/api/login"
                Log.d("PrometheanProxy", "Attempting to connect to $fullUrl")

                val client = getInsecureOkHttpClient().build()

                val json = """
                {
                    "username": "$username",
                    "password": "$password"
                }
            """.trimIndent()

                val requestBody =
                    json.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url(fullUrl)
                    .post(requestBody)
                    .build()


                client.newCall(request).execute().use { response ->
                    // .string() can only be called once, so store it in a variable.
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)

                    if (!response.isSuccessful) {
                        Log.e("PrometheanProxy", "Request failed with code: ${response.code}")

                        // Attempt to parse the error message from the JSON response
                        val errorMessage = try {
                            if (responseBody.isNullOrBlank()) {
                                "Unknown error"
                            } else {
                                // Use optString for safety; it returns an empty string if the key doesn't exist.
                                jsonObject.optString(
                                    "error",
                                    "Error response did not contain a message."
                                )
                            }
                        } catch (e: Exception) {
                            // If parsing fails, return the raw response body as the message.
                            Log.e("PrometheanProxy", "Failed to parse JSON error response", e)
                            responseBody ?: "An unknown error occurred."
                        }

                        return@withContext Pair(false, errorMessage)
                    }

                    Log.d("PrometheanProxy", "Response: $responseBody")
                    // Return a Pair indicating success and the response body
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val token = jsonObject.optString("token")

                        if (token.isNotBlank()) {
                            Log.d("PrometheanProxy", "Successfully received auth token: $token")
                            authToken = token
                            return@withContext Pair(true, token)
                        } else {
                            Log.e("PrometheanProxy", "Auth token not found in server response.")
                            return@withContext Pair(false, "Auth token not found in server response.")
                        }
                    } catch (e: Exception) {
                        Log.e("PrometheanProxy", "Failed to parse JSON success response", e)
                        return@withContext Pair(false, "Failed to parse server response.")
                    }
                }

            } catch (e: IOException) {
                Log.e("PrometheanProxy", "Error connecting to server: ${e.message}", e)
                return@withContext Pair(false, e.message)
            }
        }
    }
}