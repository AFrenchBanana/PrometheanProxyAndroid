package com.example.comprometheanproxy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


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
        val username = sharedPreferences.getString("username", "")
        val password = sharedPreferences.getString("password", "")
        if (url.isNullOrEmpty() || port.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()) {
            Log.d("Intial config invalid", "URL: $url, Port: $port, Username: $username, Password: $password")
            return false
        }
        Log.d("Intial config valid", "URL: $url")
        return true

    }

}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Dashboard", "Notifications")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Dashboard, Icons.Filled.Notifications)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Content area
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> Text("Home Screen", modifier = Modifier.padding(16.dp))
                1 -> Text("Dashboard Screen")
                2 -> Text("Notifications Screen")
            }
        }
    }
}