package com.promethean.proxy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.promethean.proxy.main.MainScreen
import com.promethean.proxy.initialBoot.LoginPage


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
                    MainScreen()
                }
            }
        }
    }

    fun initialConfigCheck(): Boolean {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val url = sharedPreferences.getString("url", "")
        val port = sharedPreferences.getString("port", "")
        val auth = sharedPreferences.getBoolean("withAuth", false)
        if (auth) {
            val username = sharedPreferences.getString("login", "")
            val password = sharedPreferences.getString("password", "")
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.d("Auth invalid", "Username: $username, Password: $password")
                return false
            }
        }
        if (url.isNullOrEmpty() || port.isNullOrEmpty()) {
            Log.d("Intial config invalid", "URL: $url, Port: $port")
            return false
        }
        Log.d("Intial config valid", "URL: $url")
        return true
    }

    }

