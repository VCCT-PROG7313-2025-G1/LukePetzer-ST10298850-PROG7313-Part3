package com.example.lukepetzer_st10298850_prog7313_part3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lukepetzer_st10298850_prog7313_part3.data.User
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    fun registerUser(user: User) {
        viewModelScope.launch {
            val success = repository.registerUser(user)
            if (success) {
                _currentUser.value = repository.getUserByUsername(user.username)
            } else {
                _loginError.value = "Username already exists"
            }
        }
    }

    fun loginUser(username: String, password: String) {
        viewModelScope.launch {
            val user = repository.loginUser(username, password)
            if (user != null) {
                _currentUser.value = user
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    fun updateLoginStreak(streak: Int, date: Long, longest: Int) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                repository.updateLoginStreak(user.userId, streak, date, longest)
                _currentUser.value = repository.getUserById(user.userId) // refresh user
            }
        }
    }
}