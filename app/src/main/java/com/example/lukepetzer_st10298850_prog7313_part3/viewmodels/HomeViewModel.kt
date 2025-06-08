package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoalDao
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryDao
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.data.TransactionDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress

class HomeViewModel(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetGoalDao: BudgetGoalDao
) : ViewModel() {

    private val _totalSpending = MutableLiveData<Double>()
    val totalSpending: LiveData<Double> = _totalSpending

    private val _remainingBudget = MutableLiveData<Double>()
    val remainingBudget: LiveData<Double> = _remainingBudget

    private val _budgetGoals = MutableLiveData<Pair<Double, Double>>()
    val budgetGoals: LiveData<Pair<Double, Double>> = _budgetGoals

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    private val _categoryTotals = MutableLiveData<Map<String, Double>>()
    val categoryTotals: LiveData<Map<String, Double>> = _categoryTotals

    private val _categoryProgress = MutableLiveData<List<CategoryProgress>>()
    val categoryProgress: LiveData<List<CategoryProgress>> = _categoryProgress

    private val _totalBudget = MutableLiveData<Double>()
    val totalBudget: LiveData<Double> = _totalBudget

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _budgetGoalData = MutableLiveData<BudgetGoal?>()
    val budgetGoalData: LiveData<BudgetGoal?> = _budgetGoalData

    private var currentUserId: Long = -1
    private var currentDateFilter = DateFilter.MONTHLY
        private set

    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    private var selectedCategory: String? = null

    fun loadUserData(userId: Long) {
        currentUserId = userId
        Log.d("HomeViewModel", "Loading user data for userId: $userId")
        viewModelScope.launch {
            loadCategories()
            updateBudgetGoals()
            updateTransactions()
            loadBudgetGoal(userId)
        }
    }

    fun setDateFilter(filter: DateFilter) {
        currentDateFilter = filter
        if (filter != DateFilter.CUSTOM) {
            customStartDate = null
            customEndDate = null
        }
        updateTransactions()
    }

    fun setCustomDateRange(startDate: Date, endDate: Date) {
        customStartDate = startDate
        customEndDate = endDate
        currentDateFilter = DateFilter.CUSTOM
        updateTransactions()
    }

    fun setCategory(category: String?) {
        selectedCategory = category
        updateTransactions()
    }

    private fun updateTransactions() {
        viewModelScope.launch {
            val (startDate, endDate) = getDateRange()
            Log.d("HomeViewModel", "Fetching transactions from $startDate to $endDate")
            val transactions = if (selectedCategory != null && selectedCategory != "All Categories") {
                transactionDao.getTransactionsForUserByCategoryInDateRange(currentUserId, selectedCategory!!, startDate, endDate)
            } else {
                transactionDao.getTransactionsForUserInDateRange(currentUserId, startDate, endDate)
            }
            Log.d("HomeViewModel", "Fetched ${transactions.size} transactions")
            _transactions.value = transactions
            updateTotalSpending(transactions)
            updateCategoryTotals(transactions)
            updateCategoryProgress(transactions)
        }
    }

    private fun updateTotalSpending(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount }
        _totalSpending.value = total
        updateRemainingBudget(total)
    }

    private fun updateRemainingBudget(totalSpending: Double) {
        viewModelScope.launch {
            val categoryBudgets = categoryDao.getCategoryBudgetsForUser(currentUserId)
            val totalBudget = categoryBudgets.sumOf { it.budgetAmount }
            _totalBudget.value = totalBudget
            _remainingBudget.value = totalBudget - totalSpending
        }
    }

    private fun updateBudgetGoals() {
        viewModelScope.launch {
            val categoryBudgets = categoryDao.getCategoryBudgetsForUser(currentUserId)
            val totalBudget = categoryBudgets.sumOf { it.budgetAmount }
            val totalSpending = _totalSpending.value ?: 0.0
            _budgetGoals.value = Pair(totalSpending, totalBudget - totalSpending)
        }
    }

    private fun updateCategoryTotals(transactions: List<Transaction>) {
        val totals = transactions.groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
        _categoryTotals.value = totals
        Log.d("HomeViewModel", "Category totals updated: $totals")
    }

    private fun updateCategoryProgress(transactions: List<Transaction>) {
        viewModelScope.launch {
            val categoryBudgets = categoryDao.getCategoryBudgetsForUser(currentUserId)
            val categorySpending = transactions.groupBy { it.category }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

            val progress = categoryBudgets.map { categoryBudget ->
                val spent = categorySpending[categoryBudget.category] ?: 0.0
                val progressPercentage = ((spent / categoryBudget.budgetAmount) * 100).toInt().coerceIn(0, 100)
                CategoryProgress(
                    category = categoryBudget.category,
                    spent = spent,
                    total = categoryBudget.budgetAmount,
                    progress = progressPercentage
                )
            }

            _categoryProgress.value = progress
            Log.d("HomeViewModel", "Category progress updated: $progress")
        }
    }

    private fun getDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        val startDate = when (currentDateFilter) {
            DateFilter.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            DateFilter.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            DateFilter.MONTHLY -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            DateFilter.CUSTOM -> customStartDate ?: calendar.time
        }

        return Pair(startDate, customEndDate ?: endDate)
    }

    fun searchTransactions(keyword: String) {
        viewModelScope.launch {
            val transactions = transactionDao.searchTransactionsByDescription(currentUserId, keyword)
            _transactions.value = transactions
            updateTotalSpending(transactions)
            updateCategoryTotals(transactions)
            updateCategoryProgress(transactions)
        }
    }

    fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getCurrentDateFilter(): DateFilter = currentDateFilter
    fun getCustomStartDate(): Date? = customStartDate
    fun getCustomEndDate(): Date? = customEndDate

    fun getCategories(): List<String> {
        return categories.value ?: emptyList()
    }

    fun loadCategories() {
        viewModelScope.launch {
            val userCategories = categoryDao.getAllCategoryNames(currentUserId)
            val allCategories = listOf("All Categories") + userCategories
            _categories.value = allCategories
            Log.d("HomeViewModel", "Categories loaded: $allCategories")
        }
    }

    fun loadBudgetGoal(userId: Long) {
        viewModelScope.launch {
            _budgetGoalData.value = budgetGoalDao.getGoalsForUser(userId)
        }
    }

    fun saveBudgetGoal(userId: Long, shortTerm: Double, maxGoal: Double) {
        viewModelScope.launch {
            budgetGoalDao.insertOrUpdateGoal(BudgetGoal(userId, shortTerm, maxGoal))
            loadBudgetGoal(userId)
        }
    }

    enum class DateFilter {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }

    class Factory(
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val budgetGoalDao: BudgetGoalDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(transactionDao, categoryDao, budgetGoalDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class CategoryProgress(
    val category: String,
    val spent: Double,
    val total: Double,
    val progress: Int
)