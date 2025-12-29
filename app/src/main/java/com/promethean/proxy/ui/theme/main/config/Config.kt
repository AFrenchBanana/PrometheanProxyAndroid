package com.promethean.proxy.ui.theme.main.config

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.R
import com.promethean.proxy.di.SettingsPrefs
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.ui.theme.login.LoginViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs private val prefs: SharedPreferences
) : ViewModel() {

    init {
        try {
            prefs.getInt("proxy_port", 8080)
        } catch (e: ClassCastException) {
            val stringVal = prefs.getString("proxy_port", "8080")
            val intVal = stringVal?.toIntOrNull() ?: 8080
            prefs.edit().remove("proxy_port").putInt("proxy_port", intVal).apply()
        }
    }

    private val _proxyEnabled = MutableStateFlow(prefs.getBoolean("proxy_enabled", false))
    val proxyEnabled: StateFlow<Boolean> = _proxyEnabled.asStateFlow()

    private val _portPreference = MutableStateFlow(prefs.getInt("proxy_port", 8080).toString())
    val portPreference: StateFlow<String> = _portPreference.asStateFlow()

    private val _hostPreference = MutableStateFlow(prefs.getString("proxy_host", "127.0.0.1") ?: "127.0.0.1")
    val hostPreference: StateFlow<String> = _hostPreference.asStateFlow()

    fun toggleProxy(enabled: Boolean) {
        _proxyEnabled.value = enabled
        prefs.edit().putBoolean("proxy_enabled", enabled).apply()
    }

    fun savePort(port: String) {
        val portInt = port.toIntOrNull() ?: 8080
        _portPreference.value = portInt.toString()
        prefs.edit().putInt("proxy_port", portInt).apply()
    }

    fun saveHost(host: String) {
        _hostPreference.value = host
        prefs.edit().putString("proxy_host", host).apply()
    }
}

sealed class SettingItem {
    abstract val name: Int
    abstract val icon: ImageVector?

    data class Toggle(
        @StringRes override val name: Int,
        override val icon: ImageVector? = null,
        val state: State<Boolean>,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItem()

    data class Number(
        @StringRes override val name: Int,
        override val icon: ImageVector? = null,
        val state: State<String>,
        val onSave: (String) -> Unit,
        val onCheck: (String) -> Boolean = { it.toDoubleOrNull() != null }
    ) : SettingItem()

    data class Text(
        @StringRes override val name: Int,
        override val icon: ImageVector? = null,
        val state: State<String>,
        val onSave: (String) -> Unit
    ) : SettingItem()
}

data class SettingGroup(
    @StringRes val title: Int,
    val items: List<SettingItem>
)


@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _proxyEnabled = MutableStateFlow(false)
    val proxyEnabled: StateFlow<Boolean> = _proxyEnabled.asStateFlow()

    private val _portPreference = MutableStateFlow("8080")
    val portPreference: StateFlow<String> = _portPreference.asStateFlow()

    private val _hostPreference = MutableStateFlow("127.0.0.1")
    val hostPreference: StateFlow<String> = _hostPreference.asStateFlow()

    fun toggleProxy(enabled: Boolean) { _proxyEnabled.value = enabled }
    fun savePort(port: String) { _portPreference.value = port }
    fun saveHost(host: String) { _hostPreference.value = host }
}

class Config {

    @Composable
    fun ConfigUI() {
        val vm: ConfigViewModel = hiltViewModel()

        val settingsData = listOf(
            SettingGroup(title = R.string.settings_first_category,
                items = listOf(
                    SettingItem.Toggle(
                        name = R.string.enable_proxy,
                        state = vm.proxyEnabled.collectAsState(),
                        onCheckedChange = { vm.toggleProxy(it) }
                    )
                )
            ),
            SettingGroup(
                title = R.string.settings_second_category,
                items = listOf(
                    SettingItem.Number(
                        name = R.string.port_title,
                        icon = Icons.Default.NetworkCheck,
                        state = vm.portPreference.collectAsState(),
                        onSave = { vm.savePort(it) }
                    ),
                    SettingItem.Text(
                        name = R.string.proxy_host,
                        state = vm.hostPreference.collectAsState(),
                        onSave = { vm.saveHost(it) }
                    )
                )
            )
        )

        SettingsScreen(groups = settingsData)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsScreen(groups: List<SettingGroup>) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.settings)) })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp)
            ) {
                groups.forEach { group ->
                    SettingsGroupUI(group)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    @Composable
    private fun SettingsGroupUI(group: SettingGroup) {
        Column {
            Text(
                text = stringResource(group.title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    group.items.forEachIndexed { index, item ->
                        SettingItemRenderer(item)
                        if (index < group.items.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingItemRenderer(item: SettingItem) {
        when (item) {
            is SettingItem.Toggle -> ToggleComp(item)
            is SettingItem.Number -> SettingsNumberComp(item)
            is SettingItem.Text -> SettingsTextComp(item)
        }
    }

    @Composable
    private fun ToggleComp(item: SettingItem.Toggle) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIcon(item.icon)
            Text(
                text = stringResource(item.name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = item.state.value,
                onCheckedChange = item.onCheckedChange
            )
        }
    }

    @Composable
    private fun SettingsNumberComp(item: SettingItem.Number) {
        EditableItem(
            name = item.name,
            icon = item.icon,
            value = item.state.value,
            keyboardType = KeyboardType.Number,
            onSave = item.onSave,
            onCheck = item.onCheck
        )
    }

    @Composable
    private fun SettingsTextComp(item: SettingItem.Text) {
        EditableItem(
            name = item.name,
            icon = item.icon,
            value = item.state.value,
            keyboardType = KeyboardType.Text,
            onSave = item.onSave
        )
    }

    @Composable
    private fun EditableItem(
        @StringRes name: Int,
        icon: ImageVector?,
        value: String,
        keyboardType: KeyboardType,
        onSave: (String) -> Unit,
        onCheck: (String) -> Boolean = { true }
    ) {
        var isDialogShown by remember { mutableStateOf(false) }

        if (isDialogShown) {
            TextEditDialog(
                name = name,
                storedValue = value,
                keyboardType = keyboardType,
                onSave = onSave,
                onCheck = onCheck,
                onDismiss = { isDialogShown = false }
            )
        }

        Surface(
            onClick = { isDialogShown = true },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(icon)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(name), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = value.ifEmpty { "Not set" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun ItemIcon(icon: ImageVector?) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }

    @Composable
    private fun TextEditDialog(
        @StringRes name: Int,
        storedValue: String,
        keyboardType: KeyboardType,
        onSave: (String) -> Unit,
        onCheck: (String) -> Boolean,
        onDismiss: () -> Unit
    ) {
        var textValue by remember { mutableStateOf(storedValue) }
        val isValid = remember(textValue) { onCheck(textValue) }

        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(name),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        isError = !isValid,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Button(
                            onClick = { onSave(textValue); onDismiss() },
                            enabled = isValid
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}
