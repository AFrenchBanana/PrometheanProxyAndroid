package com.promethean.proxy.login

import android.content.Context
import android.util.JsonToken
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.promethean.proxy.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.promethean.proxy.main.MainScreen
import com.promethean.proxy.ui.theme.Animation

class Authentication {

    @Composable
    fun LoginUI(context: Context, networkManager: NetworkManager) {
        val sharedPrefs = remember { context.getSharedPreferences("Settings", Context.MODE_PRIVATE) }
        val initialToken = sharedPrefs.getString("token", "")
        val initialExpiry = sharedPrefs.getString("tokenExpiry", "")

        val isAlreadyAuthenticated = !initialToken.isNullOrEmpty() &&
                !initialExpiry.isNullOrEmpty() &&
                initialExpiry.isValidToken()

        var authResult by remember { mutableStateOf<Boolean?>(if (isAlreadyAuthenticated) true else null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(isAlreadyAuthenticated) {
            if (!isAlreadyAuthenticated) {
                val result = login(context, networkManager)
                authResult = result.first
                errorMessage = result.second
            }
        }

        when (authResult) {
            true -> {
                Log.d("LoginForm", "Authenticated")
                MainScreen()
            }
            false -> {
                Log.d("LoginForm", "Authentication failed: $errorMessage")
                AuthFailed(
                    errorMessage = errorMessage ?: "Unknown error",
                    onConfirm = {
                        Toast.makeText(context, "Auth Failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                )
            }
            null -> {
                Animation().CircleProgressIndicator("Connecting to server")
            }
        }
    }

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

    @Composable
    fun AuthFailed(errorMessage: String, onConfirm: () -> Unit) {
        AlertDialog(
            onDismissRequest = onConfirm,
            title = { Text(text = "Authentication Failed") },
            text = { Text("Could not authenticate: $errorMessage") },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Retry")
                }
            }
        )
    }
}

fun String.isValidToken(): Boolean {
    return try {
        val expiryDate = java.time.OffsetDateTime.parse(this)
        expiryDate.isAfter(java.time.OffsetDateTime.now())
    } catch (e: Exception) {
        false
    }
}