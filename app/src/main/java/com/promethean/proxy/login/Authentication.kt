package com.promethean.proxy.login

import android.content.SharedPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.di.SettingsPrefs
import com.promethean.proxy.main.LoadMainScreen
import com.promethean.proxy.ui.theme.Animation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@Composable
fun AuthenticationScreen(
    authViewModel: Authentication = hiltViewModel()
) {
    authViewModel.AuthenticationUI()
}


@HiltViewModel
class Authentication @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs val prefs: SharedPreferences
) : ViewModel() {

    var connectionStatus by mutableStateOf<Boolean?>(null)
    var authResult by mutableStateOf<Boolean?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var retryTrigger by mutableStateOf(0)
    var isManualLoginMode by mutableStateOf(false)


    @Composable
    fun AuthenticationUI() {

        if (isManualLoginMode) {
            LoginPageUI()
            return
        }

        val initialToken = prefs.getString("token", "")
        val initialExpiry = prefs.getString("tokenExpiry", "")

        LaunchedEffect(retryTrigger) {
            connectionStatus = null
            val isConnected = networkManager.testConnection()
            connectionStatus = isConnected
        }

        val isAlreadyAuthenticated = !initialToken.isNullOrEmpty() &&
                !initialExpiry.isNullOrEmpty() &&
                initialExpiry.isValidToken()

        LaunchedEffect(connectionStatus, isAlreadyAuthenticated) {
            if (connectionStatus == true && !isAlreadyAuthenticated) {
                val result = login()
                authResult = result.first
                errorMessage = result.second
            } else if (connectionStatus == true && isAlreadyAuthenticated) {
                authResult = true
            }
        }

        when (connectionStatus) {
            null -> Animation().CircleProgressIndicator("Checking connection...")
            false -> {
                AuthFailed(
                    errorMessage = "Could not connect to server.",
                    onConfirm = { retryTrigger++ }
                )
            }
            true -> {
                when (authResult) {
                    true -> LoadMainScreen()
                    false -> AuthFailed(
                        errorMessage = errorMessage ?: "Unknown auth error",
                        onConfirm = { retryTrigger++ }
                    )
                    null -> Animation().CircleProgressIndicator("Authenticating...")
                }
            }
        }
    }

    suspend fun login(
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val username = prefs.getString("username", "")
        val password = prefs.getString("password", "")

        if (username.isNullOrEmpty()) {
            return@withContext Pair(false, "Username is empty")
        }

        val jsonString = org.json.JSONObject().apply {
            put("username", username)
            put("password", password)
        }.toString()

        val returnBody: String = try {
            networkManager.post(
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

                prefs.edit {
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
            },
            dismissButton = {
                Button(onClick = {
                    isManualLoginMode = true
                }) {
                    Text("Edit Settings")
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
