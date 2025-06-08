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
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val _monthlySummary = MutableLiveData<MonthlySummary>()
    val monthlySummary: LiveData<MonthlySummary> = _monthlySummary

    private val _budgetUsage = MutableLiveData<BudgetUsage>()
    val budgetUsage: LiveData<BudgetUsage> = _budgetUsage

    private val _monthlySpending = MutableLiveData<List<Entry>>()
    val monthlySpending: LiveData<List<Entry>> = _monthlySpending

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

    private fun getMonthlyBudget(context: Context): Double {
        val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat("monthly_budget", 5000f).toDouble() // Default: R5000
    }

    fun loadMonthlySpending(userId: Long) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startDate = calendar.time
            val endDate = Date()

            val expenses = repository.getExpensesBetweenDates(userId, startDate, endDate)
            _monthlySpending.value = getSpendingEntriesByDate(expenses)
        }
    }

    private fun getSpendingEntriesByDate(expenses: List<Transaction>): List<Entry> {
        val entries = mutableListOf<Entry>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val groupedByDate = expenses.groupBy { dateFormat.format(it.date) }

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        for ((dateString, expensesOnDate) in groupedByDate.toSortedMap()) {
            val date = dateFormat.parse(dateString)
            val x = ((date.time - startOfMonth) / (1000 * 60 * 60 * 24)).toFloat() // Days since start
            val total = expensesOnDate.sumOf { it.amount }
            entries.add(Entry(x, total.toFloat()))
        }

        // Debug logging
        Log.d("LineChart", "Entries count: ${entries.size}")
        entries.forEach {
            Log.d("LineChart", "x=${it.x}, y=${it.y}")
        }

        return entries
    }
}

data class BudgetUsage(
    val totalBudget: Double,
    val expenses: Double,
    val percentUsed: Double,
    val remaining: Double
)