package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<Transaction>
}