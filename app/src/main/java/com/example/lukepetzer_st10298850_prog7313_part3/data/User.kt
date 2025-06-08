package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "name") val name: String,
    var loginStreak: Int = 0,
    var lastLoginDate: Long = 0,
    var longestStreak: Int = 0,
    var profilePicture: String? = null
)