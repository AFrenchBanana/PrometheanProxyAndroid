package com.prometheanproxy.connectivity

import android.util.Log
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
    fun connectServer(address: String, username: String, password: String): Boolean {
        return try {
            // Ensure any previous connection is closed before creating a new one.
            socket?.close()

            val parts = address.split(":")
            if (parts.size != 2) {
                Log.e("Connectivity", "Invalid address format. Expected host:port")
                return false
            }
            val host = parts[0]
            val port = parts[1].toInt()

            // Create a new socket and connect to the server.
            // The constructor blocks until the connection is established or an error occurs.
            socket = Socket(host, port)
            Log.d("Connectivity", "Successfully connected to $address")
            true
        } catch (e: IOException) {
            Log.e("Connectivity", "Error connecting to server: ${e.message}", e)
            false
        } catch (e: NumberFormatException) {
            Log.e("Connectivity", "Invalid port number in address: $address", e)
            false
        } catch (e: Exception) {
            Log.e("Connectivity", "An unexpected error occurred during connection", e)
            false
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