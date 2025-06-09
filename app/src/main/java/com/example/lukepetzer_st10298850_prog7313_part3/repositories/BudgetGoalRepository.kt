package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BudgetGoalRepository(private val db: FirebaseFirestore) {

    private val goalsRef = db.collection("budget_goals")

    suspend fun getGoalsForUser(userId: String): BudgetGoal? {
        val doc = goalsRef.document(userId).get().await()
        return if (doc.exists()) {
            doc.toObject(BudgetGoal::class.java)
        } else {
            null
        }
    }

    suspend fun insertOrUpdateGoal(goal: BudgetGoal) {
        // Use userId as the document ID
        goalsRef.document(goal.userId).set(goal).await()
    }
}