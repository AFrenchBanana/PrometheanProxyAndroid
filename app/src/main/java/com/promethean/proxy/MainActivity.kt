package com.promethean.proxy

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.promethean.proxy.login.AuthenticationScreen
import com.promethean.proxy.login.LoginPageUI
import com.promethean.proxy.validation.validPort
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.AndroidEntryPoint


@HiltAndroidApp
class PrometheanProxy : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isConfigured = initialConfigCheck()

        setContent {
            MaterialTheme {
                if (!isConfigured) {
                    LoginPageUI()
                } else {
                    Log.d("MainActivity", "Starting main screen")
                    AuthenticationScreen()
                }
            }
        }
    }

    fun initialConfigCheck(): Boolean {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val url = sharedPreferences.getString("url", "")
        val port = sharedPreferences.getInt("port", 0)
        val username = sharedPreferences.getString("username", "")
        val password = sharedPreferences.getString("password", "")
        val withAuth = sharedPreferences.getBoolean("withAuth", false)

        if (url.isNullOrEmpty() || !port.validPort()) {
            Log.d(
                "Initial config invalid",
                "URL: $url, Port: $port"
            )
            return false
        }
        if (withAuth) {
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.d(
                    "Initial config invalid",
                    "Username: $username, Password: $password"
                )
                return false
            }
        }
        Log.d("Initial config valid", "URL: $url")
        return true
    }
}
