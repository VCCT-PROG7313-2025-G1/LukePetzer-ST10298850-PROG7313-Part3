package com.example.lukepetzer_st10298850_prog7313_part3.repositories

import com.example.lukepetzer_st10298850_prog7313_part3.data.BudgetGoal
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BudgetGoalRepository(private val db: FirebaseFirestore) {

    private val goalsRef = db.collection("users")

    suspend fun getGoalsForUser(userId: String): BudgetGoal? {
        val doc = goalsRef.document(userId).collection("goals").document("goal").get().await()
        if (!doc.exists()){
            return null
        }
        return doc.toObject(BudgetGoal::class.java)
//        return if (doc.exists()) {
//            doc.toObject(BudgetGoal::class.java)
//        } else {
//            null
//        }
    }

    suspend fun insertOrUpdateGoal(goal: BudgetGoal) {
        // Use userId as the document ID
        goalsRef.document(goal.userId).collection("goals").document("goal").set(goal).await()
    }
}