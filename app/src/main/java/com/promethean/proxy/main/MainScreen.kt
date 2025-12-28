package com.promethean.proxy.main

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.di.SettingsPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


@Composable
fun LoadMainScreen(
    mainScreenViewModel: MainScreens = hiltViewModel()
) {
    mainScreenViewModel.MainScreen()
}


@HiltViewModel
class MainScreens @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs val prefs: SharedPreferences
) : ViewModel() {


    @Composable
    fun MainScreen() {
        var selectedItem by remember { mutableIntStateOf(0) }
        val items = listOf("Home", "Dashboard", "Config")
        val icons = listOf(Icons.Filled.Home, Icons.Filled.Dashboard, Icons.Filled.Settings)
        var httptext by remember { mutableStateOf<String?>("") }

        val config = Config()

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
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedItem) {
                    0 -> Dashboard(
                        httpText = httptext,
                        onResultReceived = { httptext = it }
                    )

                    1 -> Text("Placeholder for Dash")
                    2 -> config.ConfigUI()
                }
            }
        }
    }

    @Composable
    fun Dashboard(httpText: String?, onResultReceived: (String?) -> Unit) {

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val result = networkManager.getFromServer("ping")
                    withContext(Dispatchers.Main) {
                        onResultReceived(result)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResultReceived("Error: ${e.message}")
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {

            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Text(text = httpText ?: "Loading...")
        }
    }
}
