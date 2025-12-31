package com.promethean.proxy.ui.theme.style

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Default
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.promethean.proxy.di.PreferenceRepository
import com.promethean.proxy.di.SettingsPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeStyle { BLUE, PURPLE, RED }
enum class ThemeMode { SYSTEM, LIGHT, DARK}

// 1. Blue Schemes
private val BlueDark = darkColorScheme(primary = Blue80, secondary = BlueGrey80, tertiary = Sky80)
private val BlueLight = lightColorScheme(primary = Blue40, secondary = BlueGrey40, tertiary = Sky40)

// 2. Purple Schemes
private val PurpleDark = darkColorScheme(primary = PurplePrimary80, secondary = PurpleSecondary80, tertiary = PurpleTertiary80)
private val PurpleLight = lightColorScheme(primary = PurplePrimary40, secondary = PurpleSecondary40, tertiary = PurpleTertiary40)

// 3. Red Schemes
private val RedDark = darkColorScheme(primary = Red80, secondary = Rose80, tertiary = Coral80)
private val RedLight = lightColorScheme(primary = Red40, secondary = Rose40, tertiary = Coral40)

data class ThemeSettings(
    val style: ThemeStyle = ThemeStyle.BLUE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false
)

@HiltViewModel
class PrometheanProxyTheme @Inject constructor(
    private val prefs: PreferenceRepository
) : ViewModel() {

    val themeState: StateFlow<ThemeSettings> = combine(
        prefs.themeFlow,
        prefs.themeModeFlow
    ) { style, mode ->
        ThemeSettings(style = style, themeMode = mode)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSettings()
        )

    fun updateThemeStyle(newStyle: ThemeStyle) {
        viewModelScope.launch {
            prefs.setTheme(newStyle)
        }
    }

    fun updateThemeMode(newMode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(newMode)
        }
    }

    @Composable
    fun PrometheanTheme(
        content: @Composable () -> Unit
    ) {
        val settings by themeState.collectAsStateWithLifecycle()

        val isDark = when (settings.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

        val colorScheme = when {
            settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> when (settings.style) {
                ThemeStyle.BLUE -> if (isDark) BlueDark else BlueLight
                ThemeStyle.PURPLE -> if (isDark) PurpleDark else PurpleLight
                ThemeStyle.RED -> if (isDark) RedDark else RedLight
            }
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}@Composable
fun ThemeSwitcher(
    currentStyle: ThemeStyle, // Fixed the package path mismatch
    onStyleChange: (ThemeStyle) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Theme Color", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (currentStyle == style) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
