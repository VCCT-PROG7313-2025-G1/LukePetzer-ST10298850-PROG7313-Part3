package com.example.lukepetzer_st10298850_prog7313_part3.data
import androidx.room.*

@Dao
interface BudgetGoalDao {
    @Query("SELECT * FROM budget_goals WHERE userId = :userId LIMIT 1")
    suspend fun getGoalsForUser(userId: Long): BudgetGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: BudgetGoal)
}