package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TransactionHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    fun loadTransactions(userId: Long, startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _transactions.value = transactionDao.getTransactionsForUserInDateRange(userId, startDate, endDate)
        }
    }

    fun loadTransactionsForLastDays(userId: Long, days: Int) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        loadTransactions(userId, startDate, endDate)
    }

    fun loadAllTransactions(userId: Long) {
        viewModelScope.launch {
            _transactions.value = transactionDao.getTransactionsForUser(userId)
        }
    }
}