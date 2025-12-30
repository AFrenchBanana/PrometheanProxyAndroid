package com.promethean.proxy.ui.theme.style

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ThemeStyle { BLUE, PURPLE, RED }

// 1. Blue Schemes
private val BlueDark = darkColorScheme(primary = Blue80, secondary = BlueGrey80, tertiary = Sky80)
private val BlueLight = lightColorScheme(primary = Blue40, secondary = BlueGrey40, tertiary = Sky40)

// 2. Purple Schemes
private val PurpleDark = darkColorScheme(primary = PurplePrimary80, secondary = PurpleSecondary80, tertiary = PurpleTertiary80)
private val PurpleLight = lightColorScheme(primary = PurplePrimary40, secondary = PurpleSecondary40, tertiary = PurpleTertiary40)

// 3. Red Schemes
private val RedDark = darkColorScheme(primary = Red80, secondary = Rose80, tertiary = Coral80)
private val RedLight = lightColorScheme(primary = Red40, secondary = Rose40, tertiary = Coral40)

@Composable
fun PrometheanProxyAndroidTheme(
    style: ThemeStyle = ThemeStyle.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (style) {
            ThemeStyle.BLUE -> if (darkTheme) BlueDark else BlueLight
            ThemeStyle.PURPLE -> if (darkTheme) PurpleDark else PurpleLight
            ThemeStyle.RED -> if (darkTheme) RedDark else RedLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ThemeSwitcher(
    currentStyle: ThemeStyle,
    onStyleChange: (ThemeStyle) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Theme Color", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(style.name) }
                )
            }
        }
    }
}