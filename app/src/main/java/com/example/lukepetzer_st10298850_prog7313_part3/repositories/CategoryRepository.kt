package com.example.lukepetzer_st10298850_prog7313_part3.repositories


import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryDao

class CategoryRepository(private val categoryDao: CategoryDao) {
    suspend fun getCategoriesForUser(userId: Long): List<Category> {
        return categoryDao.getCategoriesForUser(userId)
    }

    suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}