package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.MonthlySummary
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.*

class TransactionRepository(private val db: FirebaseFirestore) {

    private val transactionsRef = db.collection("transactions")

    suspend fun insertTransaction(transaction: Transaction) {
        val docRef = transactionsRef.document()
        transaction.id = docRef.id
        docRef.set(transaction).await()
    }

    suspend fun getAllTransactions(): List<Transaction> {
        val snapshot = transactionsRef
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(Transaction::class.java)
    }

    suspend fun getTransactionsForUser(userId: String): List<Transaction> {
        val snapshot = db.collection("users").document(userId).collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull {
            it.toObject(Transaction::class.java)
//            .apply {
//            this?.date = it.getLong("date")!!
         }
    }

    suspend fun getTransactionsForUserInDateRange(userId: String, startDate: Date, endDate: Date): List<Transaction> {
        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startDate.time)
            .whereLessThanOrEqualTo("date", endDate.time)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(Transaction::class.java)
    }

    suspend fun getTransactionsByCategoryInDateRange(userId: String, category: String, startDate: Date, endDate: Date): List<Transaction> {
        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .whereGreaterThanOrEqualTo("date", startDate.time)
            .whereLessThanOrEqualTo("date", endDate.time)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(Transaction::class.java)
    }

    suspend fun searchTransactionsByDescription(userId: String, keyword: String): List<Transaction> {
        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.toObjects(Transaction::class.java).filter {
            it.description?.contains(keyword, ignoreCase = true) == true
        }
    }

    suspend fun getTotalIncomeForPeriod(userId: String, startDate: Date, endDate: Date): Double {
        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "Income")
            .whereGreaterThanOrEqualTo("date", startDate.time)
            .whereLessThanOrEqualTo("date", endDate.time)
            .get()
            .await()

        return snapshot.toObjects(Transaction::class.java).sumOf { it.amount }
    }

    suspend fun getTotalExpensesForPeriod(userId: String, startDate: Date, endDate: Date): Double {
        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "Expense")
            .whereGreaterThanOrEqualTo("date", startDate.time)
            .whereLessThanOrEqualTo("date", endDate.time)
            .get()
            .await()

        return snapshot.toObjects(Transaction::class.java).sumOf { it.amount }
    }

    suspend fun getWeeklySpending(userId: String): List<Transaction> {
        val endDate = Date()
        val startDate = Calendar.getInstance().apply {
            time = endDate
            add(Calendar.DAY_OF_YEAR, -7)
        }.time

        val snapshot = transactionsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "Expense")
            .whereGreaterThanOrEqualTo("date", startDate.time)
            .whereLessThanOrEqualTo("date", endDate.time)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.toObjects(Transaction::class.java)
    }

    suspend fun getMonthlySummary(userId: String): MonthlySummary {
        val currentDate = Date()
        val startOfMonth = getStartOfMonth(currentDate)
        val endOfMonth = getEndOfMonth(currentDate)

        val income = getTotalIncomeForPeriod(userId, startOfMonth, endOfMonth)
        val expenses = getTotalExpensesForPeriod(userId, startOfMonth, endOfMonth)
        return MonthlySummary(income, expenses, income - expenses)
    }

    private fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
}

data class MonthlySummary(
    val income: Double,
    val expenses: Double,
    val remaining: Double
)