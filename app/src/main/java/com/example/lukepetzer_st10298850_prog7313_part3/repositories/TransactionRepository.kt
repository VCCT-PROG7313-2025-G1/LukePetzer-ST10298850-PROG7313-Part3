package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.data.TransactionDao
import java.util.Calendar
import java.util.Date

class TransactionRepository(private val transactionDao: TransactionDao) {

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getMonthlySummary(userId: Long): MonthlySummary {
        val currentDate = Date()
        val startOfMonth = getStartOfMonth(currentDate)
        val endOfMonth = getEndOfMonth(currentDate)

        val income = transactionDao.getTotalIncomeForPeriod(userId, startOfMonth, endOfMonth) ?: 0.0
        val expenses = transactionDao.getTotalExpensesForPeriod(userId, startOfMonth, endOfMonth) ?: 0.0
        val remaining = income - expenses

        return MonthlySummary(income, expenses, remaining)
    }

    suspend fun getWeeklySpending(userId: Long): List<Transaction> {
        val endDate = Date()
        val startDate = Calendar.getInstance().apply {
            time = endDate
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        return transactionDao.getExpensesBetweenDates(userId, startDate, endDate)
    }

    suspend fun getExpensesBetweenDates(userId: Long, startDate: Date, endDate: Date): List<Transaction> {
        return transactionDao.getExpensesBetweenDates(userId, startDate, endDate)
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