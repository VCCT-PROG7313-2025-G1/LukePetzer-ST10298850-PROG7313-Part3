package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {

    private val repository = CategoryRepository()

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    fun loadCategories(userId: String) {
        viewModelScope.launch {
            val categoryList = repository.getCategoriesForUser(userId)
            _categories.postValue(categoryList)
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.addCategory(category)
            loadCategories(category.userId)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            loadCategories(category.userId)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category.id)
            loadCategories(category.userId)
        }
    }
}