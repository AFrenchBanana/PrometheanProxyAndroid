package com.promethean.proxy.ui.theme.main.config

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.promethean.proxy.R
import com.promethean.proxy.di.PreferenceRepository
import com.promethean.proxy.validation.validPort
import com.promethean.proxy.validation.isValidAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- DATA MODELS ---

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
        val onCheck: (String) -> Boolean = { it.toIntOrNull() != null }
    ) : SettingItem()

    data class Text(
        @StringRes override val name: Int,
        override val icon: ImageVector? = null,
        val state: State<String>,
        val onSave: (String) -> Unit,
        val onCheck: (String) -> Boolean = { true }
    ) : SettingItem()

    /** Use this to navigate to a new settings screen */
    data class SubMenu(
        @StringRes override val name: Int,
        override val icon: ImageVector? = null,
        val route: String
    ) : SettingItem()
}

data class SettingGroup(
    @StringRes val title: Int,
    val items: List<SettingItem>
)

// --- VIEWMODEL ---

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val prefs: PreferenceRepository
) : ViewModel() {

    private val _toggleAuth = MutableStateFlow(false)
    val toggleAuth = _toggleAuth.asStateFlow()

    private val _portPreference = MutableStateFlow("")
    val portPreference = _portPreference.asStateFlow()

    private val _urlPreference = MutableStateFlow("")
    val urlPreference = _urlPreference.asStateFlow()

    init {
        viewModelScope.launch {
            _portPreference.value = prefs.getPort().toString()
            _urlPreference.value = prefs.getUrl()
            _toggleAuth.value = prefs.getWithAuth()
        }
    }

    fun toggleAuth(enabled: Boolean) {
        viewModelScope.launch { prefs.setWithAuth(enabled) }
        _toggleAuth.value = enabled
    }

    fun savePort(portStr: String) {
        val port = portStr.toIntOrNull() ?: 0
        if (port.validPort()) {
            _portPreference.value = portStr
            viewModelScope.launch { prefs.setPort(port) }
        }
    }

    fun saveUrl(url: String) {
        if (url.isValidAddress()) {
            _urlPreference.value = url
            viewModelScope.launch { prefs.setUrl(url) }
        }
    }

}


@Composable
fun ConfigUI() {
    val navController = rememberNavController()
    val vm: ConfigViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "root") {
        composable("root") {
            SettingsListScreen(
                title = R.string.settings,
                groups = listOf(
                    SettingGroup(
                        title = R.string.settings_first_category,
                        items = listOf(
                            SettingItem.Toggle(
                                name = R.string.enable_auth,
                                state = vm.toggleAuth.collectAsState(),
                                icon = Icons.Default.Security,
                                onCheckedChange = { vm.toggleAuth(it) }
                            ),
                            SettingItem.SubMenu(
                                name = R.string.network_category,
                                icon = Icons.Default.NetworkCheck,
                                route = "network"
                            ),
                            SettingItem.SubMenu(
                            name = R.string.about,
                            icon = Icons.AutoMirrored.Filled.Help,
                            route = "about"
                        )
                        )
                    )
                ),
                navController = navController
            )
        }

        composable("network") {
            SettingsListScreen(
                title = R.string.network_category,
                showBack = true,
                groups = listOf(
                    SettingGroup(
                        title = R.string.port_title,
                        items = listOf(
                            SettingItem.Number(
                                name = R.string.port_title,
                                state = vm.portPreference.collectAsState(),
                                onSave = { vm.savePort(it) },
                                onCheck = { it.toIntOrNull()?.validPort() == true }
                            ),
                            SettingItem.Text(
                                name = R.string.proxy_host,
                                state = vm.urlPreference.collectAsState(),
                                onSave = { vm.saveUrl(it) },
                                onCheck = { it.isValidAddress() }
                            )
                        )
                    )
                ),
                navController = navController
            )
        }

        composable("about") {
            SettingsListScreen(
                title = R.string.about,
                showBack = true,
                groups = listOf(

                ),
                navController = navController
            )
        }
    }
}

// --- REUSABLE UI COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListScreen(
    @StringRes title: Int,
    groups: List<SettingGroup>,
    navController: NavController,
    showBack: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) // Use actual Back icon here
                        }
                    }
                }
            )
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
                            SettingItemRenderer(item, navController)
                            if (index < group.items.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingItemRenderer(item: SettingItem, navController: NavController) {
    when (item) {
        is SettingItem.Toggle -> ToggleComp(item)
        is SettingItem.Number -> EditableItem(
            name = item.name, icon = item.icon, value = item.state.value,
            keyboardType = KeyboardType.Number, onSave = item.onSave, onCheck = item.onCheck
        )
        is SettingItem.Text -> EditableItem(
            name = item.name, icon = item.icon, value = item.state.value,
            keyboardType = KeyboardType.Text, onSave = item.onSave
        )
        is SettingItem.SubMenu -> SubMenuComp(item, navController)
    }
}

@Composable
private fun SubMenuComp(item: SettingItem.SubMenu, navController: NavController) {
    Surface(
        onClick = { navController.navigate(item.route) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIcon(item.icon)
            Text(stringResource(item.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleComp(item: SettingItem.Toggle) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIcon(item.icon)
        Text(stringResource(item.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = item.state.value, onCheckedChange = item.onCheckedChange)
    }
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

    Surface(onClick = { isDialogShown = true }, color = Color.Transparent) {
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
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
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
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(name), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    isError = !isValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(textValue); onDismiss() }, enabled = isValid) { Text("Save") }
                }
            }
        }
    }
}
