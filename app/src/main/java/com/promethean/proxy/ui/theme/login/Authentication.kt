package com.promethean.proxy.ui.theme.login

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.di.PreferenceRepository
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.ui.theme.main.LoadMainScreen
import com.promethean.proxy.ui.theme.Animation
import dagger.hilt.android.lifecycle.HiltViewModel
import org.json.JSONException
import org.json.JSONObject
import java.time.OffsetDateTime
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
    private val preferenceRepository: PreferenceRepository,
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

        LaunchedEffect(retryTrigger) {
            connectionStatus = null
            val isConnected = networkManager.testConnection()
            connectionStatus = isConnected
        }

        LaunchedEffect(connectionStatus) {
            if (connectionStatus == true) {


                val initialToken = preferenceRepository.getToken()
                val initialExpiry = preferenceRepository.getTokenExpiry()

                val isAlreadyAuthenticated = initialToken.isNotEmpty() &&
                        initialExpiry.isNotEmpty() &&
                        initialExpiry.isValidToken()

                if (!isAlreadyAuthenticated) {
                    val result = login()
                    authResult = result.first
                    errorMessage = result.second
                } else {
                    authResult = true
                }
            }
        }

        // 3. UI State Rendering
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
        val username = preferenceRepository.getUsername()
        val password = preferenceRepository.getPassword()


        if (username.isNullOrEmpty()) {
            return@withContext Pair(false, "Username is empty")
        }

        val jsonString = JSONObject().apply {
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
            val jsonResult = JSONObject(returnBody)
            if (jsonResult.has("token")) {
                val token = jsonResult.getString("token")
                val expires = jsonResult.getString("expires")

                preferenceRepository.setToken(token)
                preferenceRepository.setTokenExpiry(expires)

                Pair(true, "")
            } else {
                Pair(false, "Login failed: Server did not return a token")
            }
        } catch (e: JSONException) {
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
        val expiryDate = OffsetDateTime.parse(this)
        expiryDate.isAfter(OffsetDateTime.now())
    } catch (e: Exception) {
        false
    }
}
