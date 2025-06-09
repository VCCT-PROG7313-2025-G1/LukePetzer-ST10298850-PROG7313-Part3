package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    // Register user
    suspend fun registerUser(user: User): Boolean {
        // Check for duplicate username
        val existingUser = getUserByUsername(user.username)
        if (existingUser != null) return false

        val docRef = usersCollection.document()
        user.userId = docRef.id
        docRef.set(user).await()
        return true
    }

    // Login user (basic match, assumes password is hashed)
    suspend fun loginUser(username: String, password: String): User? {
        val user = getUserByUsername(username)
        return if (user != null && user.password == password) user else null
    }

    // Get user by username
    suspend fun getUserByUsername(username: String): User? {
        val snapshot = usersCollection
            .whereEqualTo("username", username)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null
        return doc.toObject(User::class.java)
    }

    // Get user by ID
    suspend fun getUserById(userId: String): User? {
        val doc = usersCollection.document(userId).get().await()
        return doc.toObject(User::class.java)
    }

    // Update login streak
    suspend fun updateLoginStreak(userId: String, newStreak: Int, lastLoginDate: Long, longestStreak: Int) {
        val updates = mapOf(
            "loginStreak" to newStreak,
            "lastLoginDate" to lastLoginDate,
            "longestStreak" to longestStreak
        )
        usersCollection.document(userId).update(updates).await()
    }

    // Update username
    suspend fun updateUsername(userId: String, newUsername: String) {
        usersCollection.document(userId).update("username", newUsername).await()
    }

    // Update profile picture
    suspend fun updateProfilePicture(userId: String, imagePath: String) {
        usersCollection.document(userId).update("profilePicture", imagePath).await()
    }

    // [Optional] Get total budget for user (if you use a 'categories' collection)
    suspend fun getTotalBudgetForUser(userId: String): Double {
        val snapshot = db.collection("categories")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.sumOf { it.getDouble("budgetAmount") ?: 0.0 }
    }
}