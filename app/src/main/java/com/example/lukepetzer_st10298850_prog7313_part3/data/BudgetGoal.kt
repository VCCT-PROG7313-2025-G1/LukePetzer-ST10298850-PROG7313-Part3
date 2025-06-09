package com.example.lukepetzer_st10298850_prog7313_part3.data

data class BudgetGoal(
    var userId: String = "", // Can be used as the document ID
    val shortTermGoal: Double = 0.0,
    val maxGoal: Double = 0.0
)