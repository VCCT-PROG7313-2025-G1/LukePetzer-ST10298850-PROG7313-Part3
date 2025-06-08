package com.example.lukepetzer_st10298850_prog7313_part3.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey val userId: Long,
    val shortTermGoal: Double,
    val maxGoal: Double
)