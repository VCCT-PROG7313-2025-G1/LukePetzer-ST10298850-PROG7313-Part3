package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress
import com.example.lukepetzer_st10298850_prog7313_part3.data.MonthlySummary
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.BudgetGoalRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.TransactionRepository
import com.github.mikephil.charting.data.Entry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class HomeViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val budgetGoalRepo: BudgetGoalRepository
) : ViewModel() {

//    StatsViewModel(application: Application) : AndroidViewModel(application)

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

    private val _monthlySummary = MutableLiveData<MonthlySummary>()
    val monthlySummary: LiveData<MonthlySummary> = _monthlySummary

    private val _budgetUsage = MutableLiveData<BudgetUsage>()
    val budgetUsage: LiveData<BudgetUsage> = _budgetUsage

    private val _monthlySpending = MutableLiveData<List<Entry>>()
    val monthlySpending: LiveData<List<Entry>> = _monthlySpending


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

    public fun loadTransactions() {
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
            _transactions.value =
                transactions.filter { _selectedCategory.value == null || it.category == _selectedCategory.value }
            updateCategoryProgress()
            updateRemainingBudget()
            //updateMonthlyExpenses(transactions)
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

    fun customRangeFormatted(): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return "${dateFormat.format(customStartDate)} - ${dateFormat.format(customEndDate)}"
    }



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

                _monthlySummary.value =
                    MonthlySummary(income = income, expenses = expenses, remaining = remaining)
            }
            .addOnFailureListener {
                Log.e("StatsViewModel", "Error fetching summary", it)
            }
    }

    // Fetch budget usage stats
//    fun loadBudgetUsage(userId: String) {
//        loadMonthlySummary(userId)
//
//        monthlySummary.observeForever { summary ->
//            val totalBudget = getMonthlyBudget(getApplication())
//            val expenses = summary.expenses
//            val percentUsed = min((expenses / totalBudget) * 100.0, 100.0)
//            val remaining = totalBudget - expenses
//
//            _budgetUsage.value = BudgetUsage(
//                totalBudget = totalBudget,
//                expenses = expenses,
//                percentUsed = percentUsed,
//                remaining = remaining
//            )
//        }
//    }

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

                updateMonthlyExpenses( expenses)

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


    enum class DateFilter {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }
}
