package com.promethean.proxy.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.promethean.proxy.network.NetworkManager
import com.promethean.proxy.di.SettingsPrefs
import com.promethean.proxy.validation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs private val prefs: SharedPreferences
) : ViewModel() {

    fun saveAndConnect(ip: String, port: Int, login: String, password: String, onComplete: () -> Unit) {
        prefs.edit {
            putString("url", ip)
            putInt("port", port)
            putString("username", login)
            putString("password", password)
        }

        networkManager.updateConfig()
        onComplete()
    }
}

@Composable
fun LoginPageUI(
    viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    LoginForm(viewModel, context)
}

@Composable
fun LoginForm(viewModel: LoginViewModel, context: Context) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(0) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var ipError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }
    var showNoAuthDialog by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val performConnection = {
        viewModel.saveAndConnect(ip, port, login, password) {
            Log.d("LoginForm", "Preferences saved and Config Updated")
            Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show()

            if (viewModel.networkManager.haveNetwork()) {
                submitted = true
            } else {
                Toast.makeText(context, "No network connection", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showNoAuthDialog) {
        ConfirmNoAuth(
            onConfirm = {
                showNoAuthDialog = false
                performConnection()
            },
            onDismiss = { showNoAuthDialog = false }
        )
    }

    if (submitted) {
        AuthenticationScreen()
    } else {
        Surface {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp)
            ) {
                // IP Field
                TextField(
                    value = ip,
                    onValueChange = { ip = it; ipError = null },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.NetworkCheck, null) },
                    isError = ipError != null,
                    supportingText = { if (ipError != null) Text(ipError!!) },
                    label = { Text("IP or Domain") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Port Field
                TextField(
                    value = if (port == 0) "" else port.toString(),
                    onValueChange = { if (it.all { char -> char.isDigit() }) port = it.toIntOrNull() ?: 0; portError = null },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.NetworkCheck, null) },
                    isError = portError != null,
                    supportingText = { if (portError != null) Text(portError!!) },
                    label = { Text("Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Optional: With Authentication", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Login Field
                TextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Login") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Field
                var isPasswordVisible by remember { mutableStateOf(false) }
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    label = { Text("Password") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (ip.isBlank() || !ip.isValidAddress()) {
                            ipError = "Invalid IP/Domain"
                            return@Button
                        }
                        if (!port.validPort()) {
                            portError = "Invalid Port"
                            return@Button
                        }

                        if (login.isBlank() || password.isBlank()) {
                            showNoAuthDialog = true
                        } else {
                            performConnection()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun ConfirmNoAuth(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Connection") },
        text = { Text("You haven't entered a login or password. Continue without authentication?") },
        confirmButton = { Button(onClick = onConfirm) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
