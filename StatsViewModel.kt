package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.MonthlySummary
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.TransactionRepository
import kotlinx.coroutines.launch
import android.content.Context
import com.github.mikephil.charting.data.PieEntry
import java.time.YearMonth

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val _monthlySummary = MutableLiveData<MonthlySummary>()
    val monthlySummary: LiveData<MonthlySummary> = _monthlySummary

    private val _budgetUsage = MutableLiveData<BudgetUsage>()
    val budgetUsage: LiveData<BudgetUsage> = _budgetUsage

    private val _categorySpending = MutableLiveData<List<PieEntry>>()
    val categorySpending: LiveData<List<PieEntry>> = _categorySpending

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
    }

    fun loadMonthlySummary(userId: Long) {
        viewModelScope.launch {
            _monthlySummary.value = repository.getMonthlySummary(userId)
        }
    }

    fun loadBudgetUsage(userId: Long) {
        viewModelScope.launch {
            val monthlySummary = repository.getMonthlySummary(userId)
            val totalBudget = getMonthlyBudget(getApplication())
            val expenses = monthlySummary.expenses
            val percentUsed = (expenses / totalBudget * 100).coerceAtMost(100.0)
            val remaining = totalBudget - expenses

            _budgetUsage.value = BudgetUsage(
                totalBudget = totalBudget,
                expenses = expenses,
                percentUsed = percentUsed,
                remaining = remaining
            )
        }
    }

    fun loadCategorySpending(userId: Long) {
        viewModelScope.launch {
            val expenses = repository.getExpensesForCurrentMonth(userId)
            val categoryTotals = getCategoryTotals(expenses)
            _categorySpending.value = categoryTotals.map { (category, amount) ->
                PieEntry(amount.toFloat(), category)
            }
        }
    }

    private fun getCategoryTotals(expenses: List<Expense>): Map<String, Double> {
        val currentMonth = YearMonth.now()
        return expenses
            .filter { YearMonth.from(it.date) == currentMonth }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    private fun getMonthlyBudget(context: Context): Double {
        val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat("monthly_budget", 5000f).toDouble() // Default: R5000
    }
}

data class BudgetUsage(
    val totalBudget: Double,
    val expenses: Double,
    val percentUsed: Double,
    val remaining: Double
)