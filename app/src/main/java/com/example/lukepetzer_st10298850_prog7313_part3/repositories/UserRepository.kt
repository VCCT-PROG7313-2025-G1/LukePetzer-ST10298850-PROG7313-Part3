package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.User
import com.example.lukepetzer_st10298850_prog7313_part3.data.UserDao

class UserRepository(private val userDao: UserDao) {
    suspend fun registerUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun loginUser(username: String, password: String): User? {
        return userDao.getUserByUsernameAndPassword(username, password)
    }

    suspend fun updateLoginStreak(userId: Long, streak: Int, date: Long) {
        userDao.updateLoginStreak(userId, streak, date)
    }
}