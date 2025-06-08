package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesForUser(userId: Long): List<Category>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT name, budgetAmount FROM categories WHERE userId = :userId")
    suspend fun getCategoryBudgetsForUser(userId: Long): List<CategoryBudget>

    @Query("SELECT name FROM categories WHERE userId = :userId")
    suspend fun getAllCategoryNames(userId: Long): List<String>

}

data class CategoryBudget(
    @ColumnInfo(name = "name") val category: String,
    @ColumnInfo(name = "budgetAmount") val budgetAmount: Double
)