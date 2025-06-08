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

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Long): User?

    @Query("UPDATE users SET loginStreak = :streak, lastLoginDate = :date, longestStreak = :longest WHERE userId = :userId")
    suspend fun updateLoginStreak(userId: Long, streak: Int, date: Long, longest: Int)

    @Query("SELECT SUM(budgetAmount) FROM categories WHERE userId = :userId")
    suspend fun getTotalBudgetForUser(userId: Long): Double

    @Query("UPDATE users SET username = :newUsername WHERE userId = :userId")
    suspend fun updateUsername(userId: Long, newUsername: String)

    @Query("UPDATE users SET profilePicture = :imagePath WHERE userId = :userId")
    suspend fun updateProfilePicture(userId: Long, imagePath: String)

}