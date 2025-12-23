package com.promethean.proxy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.promethean.proxy.login.LoginPage
import com.promethean.proxy.main.MainScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isConfigured = initialConfigCheck()

        setContent {
            MaterialTheme {
                if (!isConfigured) {
                    LoginPage().LoginForm(
                        context = this
                    )
                } else {
                    Log.d("MainActivity", "Starting main screen")
                    MainScreen()
                }
            }
        }
    }

    fun initialConfigCheck(): Boolean {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val url = sharedPreferences.getString("url", "")
        val port = sharedPreferences.getString("port", "")
        val username = sharedPreferences.getString("username", "")
        val password = sharedPreferences.getString("password", "")
        val withAuth = sharedPreferences.getBoolean("withAuth", false)

        if (url.isNullOrEmpty() || port.isNullOrEmpty()) {
            Log.d(
                "Intial config invalid",
                "URL: $url, Port: $port"
            )
            return false
        }
        if (withAuth) {
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.d(
                    "Intial config invalid",
                    "Username: $username, Password: $password"
                )
                return false
            }
        }
        Log.d("Intial config valid", "URL: $url")
        return true

    }

}
