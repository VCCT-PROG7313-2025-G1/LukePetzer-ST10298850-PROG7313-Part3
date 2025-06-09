package com.example.lukepetzer_st10298850_prog7313_part3.data

data class Transaction(
    var id: String = "", // Firestore document ID
    val userId: String = "",
    val type: String = "", // "Income" or "Expense"
    val amount: Double = 0.0,
    val category: String = "",
    var date: Long = 0L, // Store date as timestamp (milliseconds since epoch)
    val description: String? = null,
    val receiptUri: String? = null // Store URI or Firebase Storage download URL
)