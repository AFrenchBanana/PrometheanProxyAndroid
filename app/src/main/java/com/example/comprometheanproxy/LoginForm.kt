package com.example.comprometheanproxy

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

class LoginPage {

    @Composable
    fun IPField(
        value: String,
        onChange: (String) -> Unit,
        errorMessage: String?,
        modifier: Modifier = Modifier,
        label: String = "IP or Domain",
        placeholder: String = "Please enter the IP or domain you want to connect to"
    ) {

        val focusManager = LocalFocusManager.current
        val leadingIcon = @Composable {
            Icon(
                Icons.Default.NetworkCheck,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        TextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            isError = errorMessage != null,
            supportingText = { if (errorMessage != null) Text(errorMessage) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            placeholder = { Text(placeholder) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = VisualTransformation.None
        )
    }

    @Composable
    fun PortField(
        value: String,
        onChange: (String) -> Unit,
        errorMessage: String?,
        modifier: Modifier = Modifier,
        label: String = "Port",
        placeholder: String = "Please enter the port number"
    ) {

        val focusManager = LocalFocusManager.current
        val leadingIcon = @Composable {
            Icon(
                Icons.Default.NetworkCheck,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        TextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            isError = errorMessage != null,
            supportingText = { if (errorMessage != null) Text(errorMessage) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            placeholder = { Text(placeholder) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = VisualTransformation.None
        )
    }



    @Composable
    fun LoginField(
        value: String,
        onChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: String = "Login",
        placeholder: String = "Enter your Login"
    ) {

        val focusManager = LocalFocusManager.current
        val leadingIcon = @Composable {
            Icon(
                Icons.Default.Person,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        TextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            placeholder = { Text(placeholder) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = VisualTransformation.None
        )
    }

    @Composable
    fun PasswordField(
        value: String,
        onChange: (String) -> Unit,
        submit: () -> Unit,
        modifier: Modifier = Modifier,
        label: String = "Password",
        placeholder: String = "Enter your Password"
    ) {

        var isPasswordVisible by remember { mutableStateOf(false) }

        val leadingIcon = @Composable {
            Icon(
                Icons.Default.Key,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        val trailingIcon = @Composable {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                    if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }


        TextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Password
            ),
            keyboardActions = KeyboardActions(
                onDone = { submit() }
            ),
            placeholder = { Text(placeholder) },
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )
    }

    @Composable
    fun LoginForm(context: android.content.Context) {
        var ip by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("") }
        var login by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        var ipError by remember { mutableStateOf<String?>(null) }
        var portError by remember { mutableStateOf<String?>(null) }
        var showNoAuthDialog by remember { mutableStateOf(false) }
        var valid by remember { mutableStateOf(false) }
        var withAuth by remember { mutableStateOf(true) }
        var submitted by remember { mutableStateOf(false) }

        val savePreferences = {
            val sharedPreferences =
                context.getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE)
            sharedPreferences.edit() {
                putString("url", ip)
                putString("port", port)
                putBoolean("withAuth", withAuth)
                if (withAuth) {
                    putString("login", login)
                    putString("password", password)
                }
            }
            Log.d("LoginForm", "Preferences saved")
            Toast.makeText(context, "Preferences saved", Toast.LENGTH_SHORT).show()
            submitted = true
        }

        if (showNoAuthDialog) {
            ConfirmNoAuth(
                onConfirm = {
                    showNoAuthDialog = false
                    valid = true
                    withAuth = false
                },
                onDismiss = { showNoAuthDialog = false }
            )
        }

        if (valid) {
            savePreferences()
        }

        if (submitted) {
            MainScreen()
        } else {
            Surface {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 30.dp)
                ) {
                    IPField(
                        value = ip,
                        onChange = {
                            ip = it
                            if (it.isNotEmpty()) ipError = null
                        },
                        errorMessage = ipError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PortField(
                        value = port,
                        onChange = {
                            port = it
                            if (it.isNotEmpty()) portError = null
                        },
                        errorMessage = portError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LoginField(
                        value = login,
                        onChange = {
                            login = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    PasswordField(
                        value = password,
                        onChange = {
                            password = it
                        },
                        submit = { },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (ip.isBlank() || !ip.isValidAddress()) {
                                ipError = "invalid IP/Domain"
                                return@Button
                            }
                            if (port.isBlank() || !port.validPort()) {
                                portError = "invalid port"
                                return@Button
                            }

                            if (login.isBlank() || password.isBlank()) {
                                showNoAuthDialog = true
                            } else {
                                valid = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
    }


    @Composable
    private fun ConfirmNoAuth(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Confirm Connection") },
            text = { Text("You haven't entered a login or password. Are you sure you want to continue?") },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Continue")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }


    private fun String.validPort(): Boolean {
        val port = toIntOrNull()
        if (port == null || port < 1 || port > 65535) {
            return false
        }
        return true
    }

    private fun String.isValidAddress(): Boolean {
        val ipRegex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
        val domainRegex = Regex("""^[a-zA-Z0-9][a-zA-Z0-9-]+\.[a-zA-Z]{2,}$""")
        return matches(ipRegex) || matches(domainRegex)
    }
