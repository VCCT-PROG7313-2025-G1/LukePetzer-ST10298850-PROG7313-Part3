package com.example.lukepetzer_st10298850_prog7313_part3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import kotlinx.coroutines.launch

class AddTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    fun loadCategories(userId: Long) {
        viewModelScope.launch {
            _categories.postValue(categoryDao.getCategoriesForUser(userId))
        }
    }
}