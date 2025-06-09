package com.example.lukepetzer_st10298850_prog7313_part3.data

data class Category(
    var id: String = "", // Firestore document ID
    val name: String = "",
    val userId: String = "", // Store user's document ID
    val budgetAmount: Double = 0.0
)