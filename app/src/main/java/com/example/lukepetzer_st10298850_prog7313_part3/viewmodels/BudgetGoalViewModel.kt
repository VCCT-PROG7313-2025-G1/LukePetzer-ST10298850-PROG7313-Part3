package com.example.lukepetzer_st10298850_prog7313_part3.viewmodel

import androidx.lifecycle.*
import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.BudgetGoalRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class BudgetGoalViewModel : ViewModel() {

    private val repository = BudgetGoalRepository(FirebaseFirestore.getInstance())

    private val _budgetGoal = MutableLiveData<BudgetGoal?>()
    val budgetGoal: LiveData<BudgetGoal?> get() = _budgetGoal

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchGoal(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val goal = repository.getGoalsForUser(userId)
                _budgetGoal.value = goal
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveOrUpdateGoal(goal: BudgetGoal) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.insertOrUpdateGoal(goal)
                _budgetGoal.value = goal
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}