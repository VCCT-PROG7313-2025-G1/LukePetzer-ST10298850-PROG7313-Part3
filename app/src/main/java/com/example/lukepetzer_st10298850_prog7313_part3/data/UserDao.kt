package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.*

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun getUserByUsernameAndPassword(username: String, password: String): User?

    @Query("UPDATE users SET loginStreak = :streak, lastLoginDate = :date WHERE userId = :userId")
    suspend fun updateLoginStreak(userId: Long, streak: Int, date: Long)
}