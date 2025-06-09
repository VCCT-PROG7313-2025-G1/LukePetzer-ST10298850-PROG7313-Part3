package com.example.lukepetzer_st10298850_prog7313_part3.data

data class User(
    var userId: String = "",  // Will store Firestore document ID
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val name: String = "",
    var loginStreak: Int = 0,
    var lastLoginDate: Long = 0,
    var longestStreak: Int = 0,
    var profilePicture: String? = null
)