package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.BudgetGoalRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetGoalRepo: BudgetGoalRepository
) : ViewModel() {

    private val _userId = MutableLiveData<String>()
    val userId: LiveData<String> = _userId

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _budgetGoal = MutableLiveData<BudgetGoal?>()
    val budgetGoal: LiveData<BudgetGoal?> = _budgetGoal

    private val _remainingBudget = MutableLiveData<Double>()
    val remainingBudget: LiveData<Double> = _remainingBudget

    private val _categoryProgress = MutableLiveData<List<CategoryProgress>>()
    val categoryProgress: LiveData<List<CategoryProgress>> = _categoryProgress

    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    val categoryTotals: LiveData<Map<String, Double>> = _transactions.map { txList ->
        txList.groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    var currentFilter = DateFilter.DAILY
    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    fun setUserId(id: String) {
        _userId.value = id
        loadAllData()
    }

    fun setDateFilter(filter: DateFilter) {
        currentFilter = filter
        loadTransactions()
    }

    fun setCustomDateRange(start: Date, end: Date) {
        customStartDate = start
        customEndDate = end
        currentFilter = DateFilter.CUSTOM
        loadTransactions()
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        loadTransactions()
    }

    private fun loadAllData() {
        loadTransactions()
        loadCategories()
        loadBudgetGoal()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            val userId = _userId.value ?: return@launch
            val transactions = when (currentFilter) {
                DateFilter.DAILY -> {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    transactionRepo.getTransactionsForUserInDateRange(userId, today, Date())
                }
                DateFilter.WEEKLY -> {
                    val weekStart = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    transactionRepo.getTransactionsForUserInDateRange(userId, weekStart, Date())
                }
                DateFilter.MONTHLY -> {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    transactionRepo.getTransactionsForUserInDateRange(userId, monthStart, Date())
                }
                DateFilter.CUSTOM -> {
                    val start = customStartDate ?: return@launch
                    val end = customEndDate ?: return@launch
                    transactionRepo.getTransactionsForUserInDateRange(userId, start, end)
                }
            }
            _transactions.value = transactions.filter { _selectedCategory.value == null || it.category == _selectedCategory.value }
            updateCategoryProgress()
            updateRemainingBudget()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val userId = _userId.value ?: return@launch
            _categories.value = categoryRepo.getCategoriesForUser(userId)
        }
    }

    private fun loadBudgetGoal() {
        viewModelScope.launch {
            try {
                val userId = _userId.value ?: return@launch
                _budgetGoal.value = budgetGoalRepo.getGoalsForUser(userId)
                updateRemainingBudget()
            } catch (e: Exception) {
                // Log the error and potentially notify the user
                Log.e("HomeViewModel", "Error loading budget goal", e)
                // You might want to set _budgetGoal.value to null here if an error occurs
                // _budgetGoal.value = null
            }
        }
    }

    private fun updateCategoryProgress() {
        val transactions = _transactions.value ?: return
        val categories = _categories.value ?: return
        val progress = categories.map { category ->
            val spent = transactions.filter { it.category == category.name }.sumOf { it.amount }
            val progressPercentage = if (category.budgetAmount > 0) {
                (spent / category.budgetAmount * 100).toInt()
            } else {
                0
            }
            CategoryProgress(
                category = category.name,
                spent = spent,
                total = category.budgetAmount,
                progress = progressPercentage
            )
        }
        _categoryProgress.value = progress
    }

    private fun updateRemainingBudget() {
        val goal = _budgetGoal.value ?: return
        val spent = _transactions.value?.sumOf { it.amount } ?: 0.0
        _remainingBudget.value = goal.maxGoal - spent
    }

    fun saveBudgetGoal(userId: String, shortTerm: Double, max: Double) {
        viewModelScope.launch {
            val goal = BudgetGoal(userId, shortTerm, max)
            budgetGoalRepo.insertOrUpdateGoal(goal)
            _budgetGoal.value = goal
            updateRemainingBudget()
        }
    }

    fun customRangeFormatted(): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return "${dateFormat.format(customStartDate)} - ${dateFormat.format(customEndDate)}"
    }

    enum class DateFilter {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }
}