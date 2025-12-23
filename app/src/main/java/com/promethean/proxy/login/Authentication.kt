package com.promethean.proxy.login

import android.content.Context
import com.promethean.proxy.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class Authentication {

    suspend fun login(
        context: Context,
        networkManager: NetworkManager
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("username", "")
        val password = sharedPrefs.getString("password", "")

        if (username.isNullOrEmpty()) {
            return@withContext Pair(false, "Username is empty")
        }

        val jsonString = org.json.JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString()

        val returnBody: String = try {
            networkManager.postToServer(
                "api/login",
                jsonString,
                networkManager.getJsonContentType()
            )
        } catch (e: Exception) {
            return@withContext Pair(false, "Error: ${e.message}")
        }

        try {
            val jsonResult = org.json.JSONObject(returnBody)
            if (jsonResult.has("token")) {
                val token = jsonResult.getString("token")
                val expires = jsonResult.getString("expires")

                sharedPrefs.edit {
                    putString("token", token)
                        .putString("tokenExpiry", expires)
                }

                Pair(true, "")
            } else {
                Pair(false, "Login failed: Server did not return a token")
            }
        } catch (e: org.json.JSONException) {
            Pair(false, "Error parsing server response")
        }
    }



}