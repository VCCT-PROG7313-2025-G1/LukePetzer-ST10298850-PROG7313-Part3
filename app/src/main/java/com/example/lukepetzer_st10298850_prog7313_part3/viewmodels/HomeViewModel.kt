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
import com.github.mikephil.charting.data.Entry
import com.google.firebase.firestore.FirebaseFirestore
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

    private val _monthlyExpenses = MutableLiveData<Double>()
    val monthlyExpenses: LiveData<Double> = _monthlyExpenses

    val categoryTotals: LiveData<Map<String, Double>> = _transactions.map { txList ->
        txList.groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    private val db = FirebaseFirestore.getInstance()

    private val _monthlySpending = MutableLiveData<List<Entry>>()
    val monthlySpending: LiveData<List<Entry>> = _monthlySpending

    fun setUserId(id: String) {
        _userId.value = id
        loadAllData()
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
            val transactions = transactionRepo.getTransactionsForUser(userId)
            _transactions.value = transactions.filter { _selectedCategory.value == null || it.category == _selectedCategory.value }
            updateCategoryProgress()
            updateRemainingBudget()
            updateMonthlyExpenses(transactions)
        }
    }

    private fun updateMonthlyExpenses(transactions: List<Transaction>) {
        val totalExpenses = transactions
            .filter { it.type == "Expense" }
            .sumOf { it.amount }
        _monthlyExpenses.value = totalExpenses
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
                Log.e("HomeViewModel", "Error loading budget goal", e)
                _budgetGoal.value = null
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

    fun loadMonthlySpending(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.time
        val endDate = Date()

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startDate.toInstant().toEpochMilli())
            .whereLessThanOrEqualTo("date", endDate.toInstant().toEpochMilli())
            .whereEqualTo("type", "Expense")
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.mapNotNull { doc ->
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    val date = doc.getLong("date") ?: return@mapNotNull null
                    Transaction(
                        id = doc.id,
                        userId = userId,
                        amount = amount,
                        date = date,
                        type = "Expense",
                        category = doc.getString("category") ?: ""
                    )
                }

                updateMonthlyExpenses(expenses)
                _monthlySpending.value = getSpendingEntriesByDate(expenses)
            }
            .addOnFailureListener {
                Log.e("HomeViewModel", "Failed to fetch expenses", it)
            }
    }

    private fun getSpendingEntriesByDate(expenses: List<Transaction>): List<Entry> {
        val entries = mutableListOf<Entry>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val groupedByDate = expenses.groupBy { dateFormat.format(Date(it.date)) }

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        for ((dateString, expensesOnDate) in groupedByDate.toSortedMap()) {
            val date = dateFormat.parse(dateString) ?: continue
            val x = ((date.time - startOfMonth) / (1000 * 60 * 60 * 24)).toFloat()
            val total = expensesOnDate.sumOf { it.amount }
            entries.add(Entry(x, total.toFloat()))
        }

        return entries
    }
}