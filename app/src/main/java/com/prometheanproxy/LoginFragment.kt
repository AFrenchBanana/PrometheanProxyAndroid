package com.prometheanproxy.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.prometheanproxy.R
import com.prometheanproxy.connectivity.Connectivity
import com.prometheanproxy.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var errorToast: Toast? = null
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkLogin()

        binding.loginButton.setOnClickListener {
            handleLoginAttempt()
        }
    }

    private fun handleLoginAttempt() {
        binding.urlLayout.error = null
        binding.usernameLayout.error = null
        binding.passwordLayout.error = null

        val connectionAddress = binding.urlEditText.text.toString()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        var hasError = false
        if (connectionAddress.isBlank()) {
            binding.urlLayout.error = "URL cannot be empty"
            hasError = true
        }
        if (username.isBlank()) {
            binding.usernameLayout.error = "Username cannot be empty"
            hasError = true
        }
        if (password.isBlank()) {
            binding.passwordLayout.error = "Password cannot be empty"
            hasError = true
        }

        if (!hasError) {
            Log.d("LoginAttempt", "URL: $connectionAddress, Username: $username")
            val sharedPreferences = requireActivity().getSharedPreferences(
                "com.prometheanproxy.login", Context.MODE_PRIVATE)
            sharedPreferences.edit {
                putString("url", connectionAddress)
                putString("username", username)
                putString("password", password)
                apply()
            }
            performConnection(connectionAddress, username, password)
        }
    }

    private fun checkLogin() {
        Log.d("PrometheanProxy", "checkLogin() called")
        val sharedPreferences = requireActivity().getSharedPreferences(
            "com.prometheanproxy.login", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("url", null)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        if (url != null && username != null && password != null) {
            Log.d("PrometheanProxy", "Auto-Login: URL: $url, Username: $username")
            performConnection(url, username, password)

        }
    }

    private fun performConnection(url: String, username: String, password: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)

            val sharedPreferences = requireActivity().getSharedPreferences(
                "com.prometheanproxy.login", Context.MODE_PRIVATE)

            val (connected, message) = Connectivity.connectServer(url, username, password)
            Log.w("PrometheanProxy", "Connection result: $connected")

            if (isAdded) {
                if (connected) {
                    setLoading(false)
                    sharedPreferences.edit {
                        putBoolean("validConnection", true)
                        apply()
                    }
                    Log.d("PrometheanProxy", "Login successful")

                    Toast.makeText(requireContext(), Connectivity.authToken, Toast.LENGTH_LONG).show()

                    findNavController().navigate(R.id.navigation_beacons)
                } else {
                    Log.d("PrometheanProxy", "Login failed")

                    val toastMessage = if (sharedPreferences.getBoolean("validConnection", false)) {
                        "Login Failed" + if (message != null) ": $message" else ""
                    } else {
                        "Connection Failed" + if (message != null) ": $message" else ""
                    }
                    errorToast?.cancel()
                    errorToast = Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG)
                    errorToast?.show()

                    showRetryDialog(url, username, password)
                }
            }
        }
    }


    private fun showRetryDialog(url: String, username: String, password: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Connection Failed")
            .setMessage("Would you like to retry the connection?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                performConnection(url, username, password)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                setLoading(false)
                dialog.dismiss()
            }
            .show()
    }

    private fun setRetry(isRetry: Boolean) {
        binding.retryButton.isVisible = isRetry
        binding.loginFormGroup.isVisible = !isRetry
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loadingIndicator.isVisible = isLoading
        binding.loginFormGroup.isVisible = !isLoading
        binding.retryButton.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    }
