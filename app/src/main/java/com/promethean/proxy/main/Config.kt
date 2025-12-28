package com.promethean.proxy.main

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.R
import com.promethean.proxy.ui.theme.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.vector.ImageVector

class Config() {

    var preferences = Preferences()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigUI() {
        val vm: SettingsViewModel = hiltViewModel()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.settings),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                SettingsGroup(name = R.string.settings_first_category) {
                    // Note: Ensure SettingsSwitchComp and SettingsTextComp are defined elsewhere or added here
                    Text("Category 1 placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                SettingsGroup(name = R.string.settings_second_category) {
                    SettingsNumberComp(
                        name = R.string.title,
                        icon = Icons.Default.NetworkCheck,
                        iconDesc = R.string.ic_icon_description,
                        state = vm.textPreference.collectAsState(),
                        inputFilter = { text -> vm.filterNumbers(text) },
                        onSave = { finalText -> vm.saveNumber(finalText) },
                        onCheck = { text -> vm.checkNumber(text) },
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsGroup(
        @StringRes name: Int,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(stringResource(id = name), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column {
                    content()
                }
            }
        }
    }

    @Composable
    fun SettingsNumberComp(
        icon: ImageVector,
        @StringRes iconDesc: Int,
        @StringRes name: Int,
        state: State<String>,
        onSave: (String) -> Unit,
        inputFilter: (String) -> String,
        onCheck: (String) -> Boolean
    ) {
        var isDialogShown by remember { mutableStateOf(false) }

        if (isDialogShown) {
            Dialog(onDismissRequest = { isDialogShown = false }) {
                TextEditNumberDialog(name, state, inputFilter, onSave, onCheck) {
                    isDialogShown = false
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = { isDialogShown = true },
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(id = iconDesc),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(id = name),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                Divider(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    @Composable
    private fun TextEditNumberDialog(
        @StringRes name: Int,
        storedValue: State<String>,
        inputFilter: (String) -> String,
        onSave: (String) -> Unit,
        onCheck: (String) -> Boolean,
        onDismiss: () -> Unit
    ) {
        var currentInput by remember { mutableStateOf(TextFieldValue(storedValue.value)) }
        var isValid by remember { mutableStateOf(onCheck(storedValue.value)) }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(id = name), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = currentInput,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        val filteredText = inputFilter(it.text)
                        isValid = onCheck(filteredText)
                        currentInput = TextFieldValue(filteredText)
                    }
                )
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            onSave(currentInput.text)
                            onDismiss()
                        },
                        enabled = isValid
                    ) {
                        Text(stringResource(id = R.string.next))
                    }
                }
            }
        }
    }

    @HiltViewModel
    class SettingsViewModel @Inject constructor() : ViewModel() {

        private val _textPreference = MutableStateFlow("0.0")
        val textPreference: StateFlow<String> = _textPreference

        private val separatorChar = DecimalFormatSymbols.getInstance(Locale.ENGLISH).decimalSeparator

        fun filterNumbers(text: String): String = text.filter { it.isDigit() || it == separatorChar }

        fun checkNumber(text: String): Boolean {
            val value = text.toDoubleOrNull()
            return value != null && value >= 0
        }

        fun saveNumber(text: String) {
            val value = text.toDoubleOrNull() ?: 0.0
            _textPreference.value = value.toString()
        }
    }
}