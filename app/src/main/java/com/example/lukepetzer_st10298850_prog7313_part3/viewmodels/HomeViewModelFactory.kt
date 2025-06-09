package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.BudgetGoalRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.TransactionRepository

class HomeViewModelFactory(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetGoalRepo: BudgetGoalRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(transactionRepo, categoryRepo, budgetGoalRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}