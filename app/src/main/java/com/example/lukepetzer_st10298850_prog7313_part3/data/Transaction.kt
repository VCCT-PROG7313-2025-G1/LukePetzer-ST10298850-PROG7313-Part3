package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String, // "Income" or "Expense"
    val amount: Double,
    val category: String,
    val date: Date,
    val description: String?,
    val receiptUri: String? // We'll store the URI of the image as a string
)