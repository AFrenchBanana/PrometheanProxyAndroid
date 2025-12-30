package com.promethean.proxy

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.promethean.proxy.di.PreferenceRepository
import com.promethean.proxy.ui.theme.style.Animation
import com.promethean.proxy.ui.theme.login.AuthenticationScreen
import com.promethean.proxy.ui.theme.login.LoginPageUI
import com.promethean.proxy.ui.theme.style.PrometheanProxyAndroidTheme
import com.promethean.proxy.validation.validPort
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject


@HiltAndroidApp
class PrometheanProxy : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferenceRepository



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isConfigured by remember { mutableStateOf<Boolean?>(null) }
            PrometheanProxyAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                LaunchedEffect(Unit) {
                    isConfigured = initialConfigCheck()
                }

                MaterialTheme {
                    when (isConfigured) {
                        true -> AuthenticationScreen()
                        false -> LoginPageUI()
                        null -> {
                            Animation().CircleProgressIndicator()
                        }
                    }
                }}
            }
        }
    }

    suspend fun initialConfigCheck(): Boolean {
        val url = prefs.getUrl()
        val port = prefs.getPort()
        val username = prefs.getUsername()
        val password = prefs.getPassword()
        val withAuth = prefs.getWithAuth()

        if (url.isEmpty() || !port.validPort()) {
            Log.d("Initial config invalid", "URL: $url, Port: $port")
            return false
        }
        if (withAuth) {
            if (username.isEmpty() || password.isEmpty()) {
                Log.d("Initial config invalid", "Username: $username, Password: $password")
                return false
            }
        }
        Log.d("Initial config valid", "URL: $url")
        return true
    }
}