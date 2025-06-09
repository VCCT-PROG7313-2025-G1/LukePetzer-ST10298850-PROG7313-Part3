package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.TransactionRepository
import kotlinx.coroutines.launch
import java.util.*

class TransactionHistoryViewModel(
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    fun loadTransactions(userId: String, startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _transactions.value = transactionRepo.getTransactionsForUserInDateRange(userId, startDate, endDate)
        }
    }

    fun loadTransactionsForLastDays(userId: String, days: Int) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        loadTransactions(userId, startDate, endDate)
    }

    fun loadAllTransactions(userId: String) {
        viewModelScope.launch {
            _transactions.value = transactionRepo.getTransactionsForUser(userId)
        }
    }
}