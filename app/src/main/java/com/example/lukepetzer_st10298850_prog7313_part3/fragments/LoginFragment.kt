package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentLoginBinding
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.UserRepository
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    init {
        Log.d("LoginFragment", "LoginFragment instance created")
    }

    private var hasNavigated = false
    private lateinit var userRepository: UserRepository
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginFragment", "onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LoginFragment", "onCreateView called")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LoginFragment", "onViewCreated called")

        if (hasNavigated) {
            Log.d("LoginFragment", "Already navigated, skipping")
            return
        }

        userRepository = UserRepository()

        // Check if user is already logged in
        val userId = getUserSession()
        if (userId != null) {
            Log.d("LoginFragment", "User already logged in, navigating to home fragment")
            hasNavigated = true
            safeNavigateToHome()
            return
        }

        binding.btnSignIn.setOnClickListener {
            Log.d("LoginFragment", "Sign In button clicked")
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val user = userRepository.loginUser(username, password)
                        if (user != null) {
                            saveUserSession(user.userId)
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            Log.d("LoginFragment", "Navigating to home fragment")
                            safeNavigateToHome()
                        } else {
                            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("LoginFragment", "Login error", e)
                    }
                }
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvSignUp.setOnClickListener {
            Log.d("LoginFragment", "Sign Up text clicked, navigating to register fragment")
            safeNavigateToRegister()
        }

        binding.tvForgotPassword.setOnClickListener {
            Log.d("LoginFragment", "Forgot Password clicked")
            // TODO: Implement forgot password functionality
        }
    }

    private fun saveUserSession(userId: String) {
        Log.d("LoginFragment", "Saving user session for userId: $userId")
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("USER_ID", userId)
            apply()
        }
    }

    private fun getUserSession(): String? {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", null)
        Log.d("LoginFragment", "Retrieved user session: $userId")
        return if (userId.isNullOrEmpty()) null else userId
    }

    private fun safeNavigateToHome() {
        try {
            Log.d("LoginFragment", "Attempting to navigate to home fragment")
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment) //, null, NavOptions.Builder()
//                .setPopUpTo(R.id.loginFragment, false)
//                .build())
            Log.d("LoginFragment", "Navigation to home fragment successful")
        } catch (e: Exception) {
            Log.e("LoginFragment", "Navigation error: ${e.message}", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun safeNavigateToRegister() {
        try {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        } catch (e: Exception) {
            Log.e("LoginFragment", "Navigation error", e)
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LoginFragment", "onDestroyView called")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LoginFragment", "onDestroy called")
    }
}