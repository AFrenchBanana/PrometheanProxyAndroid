package com.prometheanproxy.connectivity
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Socket

class Connectivity {

    private var socket: Socket? = null

    /**
     * Connects to a server at the given address and port.
     *
     * This method must be called from a background thread.
     *
     * @param address The server address in "hostname:port" format.
     * @param username The username for authentication (currently unused).
     * @param password The password for authentication (currently unused).
     * @return `true` if the connection was successful, `false` otherwise.
     */
    suspend fun connectServer(url: String, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.e("PrometheanProxy", "Attempting to connect to https://" + url + "/api/login")
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://" + url + "/api/login")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }

                    Log.e("PrometheanProxy", response.body?.string().toString())
                    true
                }

            } catch (e: Exception) {
                false // Return false on failure
            }
        }
    }

    fun send() {
        // You can now use the 'socket' property to get an OutputStream
        // val outputStream = socket?.getOutputStream()
    }

    fun recv() {
        // You can now use the 'socket' property to get an InputStream
        // val inputStream = socket?.getInputStream()
    }

    /**
     * Closes the socket connection.
     */
    fun disconnect() {
        try {
            socket?.close()
            socket = null
            Log.d("Connectivity", "Socket disconnected.")
        } catch (e: IOException) {
            Log.e("Connectivity", "Error closing socket: ${e.message}", e)
        }
    }
}