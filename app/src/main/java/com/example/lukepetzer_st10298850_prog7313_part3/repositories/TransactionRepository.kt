package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.data.TransactionDao

class TransactionRepository(private val transactionDao: TransactionDao) {

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactions()
    }
}