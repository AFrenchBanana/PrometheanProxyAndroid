package com.prometheanproxy.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.prometheanproxy.R
import com.prometheanproxy.databinding.FragmentLoginBinding
import androidx.core.content.edit
import com.prometheanproxy.connectivity.Connectivity

class LoginFragment : Fragment() {
    var connectivity = Connectivity()
    private var _binding: FragmentLoginBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
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
                }
                val connected = connectivity.connectServer(connectionAddress, username, password)
                if (!connected) {
                    Log.d("PrometheanProxy", "Login failed")
                    Toast.makeText(requireContext(), "LoginFailed", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                findNavController().navigate(R.id.navigation_home)
            }
        }
    }

    fun checkLogin() {
        Log.d("PrometheanProxy", "checkLogin() called")
        val sharedPreferences = requireActivity().getSharedPreferences(
            "com.prometheanproxy.login", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("url", null)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        if (url != null && username != null && password != null) {
            Log.d("PrometheanProxy", "Login: URL: $url, Username: $username")
            val connected = connectivity.connectServer(url, username, password)
            if (!connected) {
                Log.d("PrometheanProxy", "Login failed")
                Toast.makeText(requireContext(), "LoginFailed", Toast.LENGTH_LONG).show()
                return
            }
            findNavController().navigate(R.id.navigation_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}