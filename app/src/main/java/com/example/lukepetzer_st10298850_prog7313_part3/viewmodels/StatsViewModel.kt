package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.data.MonthlySummary
import com.github.mikephil.charting.data.Entry
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    private val _monthlySummary = MutableLiveData<MonthlySummary>()
    val monthlySummary: LiveData<MonthlySummary> = _monthlySummary

    private val _budgetUsage = MutableLiveData<BudgetUsage>()
    val budgetUsage: LiveData<BudgetUsage> = _budgetUsage

    private val _monthlySpending = MutableLiveData<List<Entry>>()
    val monthlySpending: LiveData<List<Entry>> = _monthlySpending

    // Fetch and calculate monthly summary
    fun loadMonthlySummary(userId: String) {
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time
        val end = Date()

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", start.toInstant().toEpochMilli())
            .whereLessThanOrEqualTo("date", end.toInstant().toEpochMilli())
            .get()
            .addOnSuccessListener { result ->
                var income = 0.0
                var expenses = 0.0
                for (doc in result) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val type = doc.getString("type") ?: "expense"
                    if (type == "income") income += amount else expenses += amount
                }

                val remaining = income - expenses  // Calculate the remaining amount

                _monthlySummary.value = MonthlySummary(income = income, expenses = expenses, remaining = remaining)
            }
            .addOnFailureListener {
                Log.e("StatsViewModel", "Error fetching summary", it)
            }
    }

    // Fetch budget usage stats
    fun loadBudgetUsage(userId: String) {
        loadMonthlySummary(userId)

        monthlySummary.observeForever { summary ->
            val totalBudget = getMonthlyBudget(getApplication())
            val expenses = summary.expenses
            val percentUsed = min((expenses / totalBudget) * 100.0, 100.0)
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
        return prefs.getFloat("monthly_budget", 5000f).toDouble()
    }

    // Load spending over time (for chart)
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
                        date = date, // Convert Date to Long timestamp
                        type = "Expense",
                        category = doc.getString("category") ?: ""
                    )
                }

                _monthlySpending.value = getSpendingEntriesByDate(expenses)
            }
            .addOnFailureListener {
                Log.e("StatsViewModel", "Failed to fetch expenses", it)
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
            val date = dateFormat.parse(dateString) ?: continue
            val x = ((date.time - startOfMonth) / (1000 * 60 * 60 * 24)).toFloat()
            val total = expensesOnDate.sumOf { it.amount }
            entries.add(Entry(x, total.toFloat()))
        }

        // Debug logs
        Log.d("LineChart", "Entries count: ${entries.size}")
        entries.forEach { Log.d("LineChart", "x=${it.x}, y=${it.y}") }

        return entries
    }
}

data class BudgetUsage(
    val totalBudget: Double,
    val expenses: Double,
    val percentUsed: Double,
    val remaining: Double
)