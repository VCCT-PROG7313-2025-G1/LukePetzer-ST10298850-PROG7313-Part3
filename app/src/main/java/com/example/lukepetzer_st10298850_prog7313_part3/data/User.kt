package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val username: String,
    val email: String,
    val password: String,
    val name: String,
    var loginStreak: Int = 0,
    var lastLoginDate: Long = 0
)