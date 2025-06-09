package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddTransactionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    fun loadCategories(activity: FragmentActivity) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", "")

        db.collection("categories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val categoryList = result.mapNotNull { it.getString("name") }
                _categories.postValue(categoryList)
            }
            .addOnFailureListener {
                _categories.postValue(emptyList())
            }
    }
}