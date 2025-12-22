package com.promethean.proxy.main

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.promethean.proxy.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun MainScreen() {

    Log.d("proxy","init")


    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Dashboard", "Config")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Dashboard, Icons.Filled.Settings)
    var httptext by remember { mutableStateOf<String?>("") }





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
                0 -> Dashboard(
                    httpText = httptext,
                    onResultReceived = { httptext = it }
                )1 -> Text("Dashboard Screen")
                2 -> Text("Config")
            }
        }
    }
}

@Composable
fun Dashboard(httpText: String?, onResultReceived: (String?) -> Unit) {
    val context = LocalContext.current
    val networkManager = remember { NetworkManager(context) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val result = networkManager.getFromServer("ping")
                withContext(Dispatchers.Main) {
                    onResultReceived(result)
                }
                Log.d("MAIN SCREEN", "Result: $result")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResultReceived("Error: ${e.message}")
                }
            }
        }
    }

    // This prints the response to the screen
    Box(modifier = Modifier.padding(16.dp)) {
        Text(text = httpText ?: "Loading...")
    }


}