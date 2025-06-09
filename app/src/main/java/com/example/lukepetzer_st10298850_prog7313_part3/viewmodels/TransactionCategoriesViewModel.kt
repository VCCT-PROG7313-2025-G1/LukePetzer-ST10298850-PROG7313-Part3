package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionCategoriesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    fun loadCategories(userId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("categories")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                val categoryList = snapshot.toObjects(Category::class.java)
                _categories.value = categoryList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            try {
                db.collection("categories").add(category).await()
                loadCategories(category.userId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}