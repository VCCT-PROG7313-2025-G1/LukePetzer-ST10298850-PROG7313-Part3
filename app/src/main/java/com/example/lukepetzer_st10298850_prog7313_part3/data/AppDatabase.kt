package com.example.lukepetzer_st10298850_prog7313_part3.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [User::class, Category::class, Transaction::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(SeedDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabase", "onCreate callback triggered")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    seedDatabase(getDatabase(context))
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error seeding database", e)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d("AppDatabase", "onOpen callback triggered")
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                if (isDatabaseEmpty(database)) {
                    Log.d("AppDatabase", "Database is empty, seeding now")
                    try {
                        seedDatabase(database)
                    } catch (e: Exception) {
                        Log.e("AppDatabase", "Error seeding database", e)
                    }
                } else {
                    Log.d("AppDatabase", "Database is not empty, skipping seeding")
                }
            }
        }

        private suspend fun isDatabaseEmpty(database: AppDatabase): Boolean {
            val userCount = database.userDao().getUserCount()
            Log.d("AppDatabase", "User count: $userCount")
            return userCount == 0
        }

        suspend fun seedDatabase(database: AppDatabase) {
            val userDao = database.userDao()
            val categoryDao = database.categoryDao()

            // Seed users
            val user1Id = userDao.insertUser(User(username = "test1", email = "test1@example.com", password = "Test123$", name = "Test User 1"))
            val user2Id = userDao.insertUser(User(username = "test2", email = "test2@example.com", password = "Test123$", name = "Test User 2"))
            val user3Id = userDao.insertUser(User(username = "test3", email = "test3@example.com", password = "Test123$", name = "Test User 3"))

            Log.d("AppDatabase", "Seeded users with IDs: $user1Id, $user2Id, $user3Id")

            // Seed categories for each user
            val categories = listOf("Food", "Transport", "Entertainment", "Utilities", "Savings")
            for (userId in listOf(user1Id, user2Id, user3Id)) {
                categories.forEach { categoryName ->
                    val categoryId = categoryDao.insertCategory(Category(name = categoryName, userId = userId, budgetAmount = 500.0))
                    Log.d("AppDatabase", "Seeded category '$categoryName' for user $userId with ID: $categoryId")
                }
            }
        }
    }
}