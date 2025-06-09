package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val categoriesCollection = db.collection("categories")

    suspend fun getCategoriesForUser(userId: String): List<Category> {
        return try {
            categoriesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Category::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCategory(category: Category): Boolean {
        return try {
            val newDoc = categoriesCollection.document()
            val categoryWithId = category.copy(id = newDoc.id)
            newDoc.set(categoryWithId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateCategory(category: Category): Boolean {
        return try {
            categoriesCollection.document(category.id).set(category).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            categoriesCollection.document(categoryId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}