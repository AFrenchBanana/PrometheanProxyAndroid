package com.promethean.proxy.ui.theme.main

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.di.SettingsPrefs
import com.promethean.proxy.ui.theme.main.config.Config
import com.promethean.proxy.ui.theme.style.PrometheanProxyTheme
import com.promethean.proxy.ui.theme.style.ThemeMode
import com.promethean.proxy.ui.theme.style.ThemeSwitcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Composable
fun LoadMainScreen(
    mainScreenViewModel: MainScreens = hiltViewModel(),
    themeViewModel: PrometheanProxyTheme = hiltViewModel()
) {
    MainScreenContent(mainScreenViewModel, themeViewModel)
}

@HiltViewModel
class MainScreens @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs val prefs: SharedPreferences
) : ViewModel() {
}

@Composable
fun MainScreenContent(
    viewModel: MainScreens,
    themeViewModel: PrometheanProxyTheme
) {
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
                    viewModel = viewModel,
                    themeViewModel = themeViewModel,
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
fun Dashboard(
    viewModel: MainScreens,
    themeViewModel: PrometheanProxyTheme,
    httpText: String?,
    onResultReceived: (String?) -> Unit
) {
    val settings by themeViewModel.themeState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val result = viewModel.networkManager.getFromServer("ping")
                withContext(Dispatchers.Main) { onResultReceived(result) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResultReceived("Error: ${e.message}") }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ThemeSwitcher(
            currentStyle = settings.style,
            onStyleChange = { selectedStyle ->
                themeViewModel.updateThemeStyle(selectedStyle)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = {
                val nextMode = when (settings.themeMode) {
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                }
                themeViewModel.updateThemeMode(nextMode)
            }) {
                Text("Theme Mode: ${settings.themeMode.name}")
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Text(text = httpText ?: "Loading...")
        }
    }
}
