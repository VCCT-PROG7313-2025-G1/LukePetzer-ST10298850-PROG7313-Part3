package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentProfileBinding
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var lastKnownStreak = 0

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // to enable onOptionsItemSelected
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        val userId = getUserIdFromSharedPreferences()
        if (userId != null) {
            loadUserProfile(userId)
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        setupClickListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        binding.btnAchievements.setOnClickListener {
            showAchievementsDialog()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showAchievementsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Achievements")
            .setMessage("🏅 Streak Badges:\n3+ days: Bronze\n5+ days: Silver\n10+ days: Gold\n20+ days: Diamond")
            .setPositiveButton("OK", null)
            .create()
            .show()
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val btnChangePhoto = dialogView.findViewById<Button>(R.id.btnChangePhoto)

        // Pre-fill username without '@'
        binding.tvUsername.text?.let {
            etUsername.setText(it.toString().removePrefix("@"))
        }

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newUsername = etUsername.text.toString()
                val userId = getUserIdFromSharedPreferences()
                if (userId != null) {
                    updateProfile(userId, newUsername)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun getUserIdFromSharedPreferences(): String? {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", null)
        return userId
    }

    private fun loadUserProfile(userId: String) {
        lifecycleScope.launchWhenStarted {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val userData = doc.data ?: return@launchWhenStarted
                    updateUIFromFirestore(userData, userId)
                } else {
                    Toast.makeText(context, "User profile not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun updateUIFromFirestore(userData: Map<String, Any>, userId: String) {
        withContext(Dispatchers.Main) {
            val name = userData["name"] as? String ?: ""
            val username = userData["username"] as? String ?: ""
            val loginStreak = (userData["loginStreak"] as? Long)?.toInt() ?: 0
            val longestStreak = (userData["longestStreak"] as? Long)?.toInt() ?: 0
            val lastLoginTimestamp = userData["lastLoginDate"] as? Timestamp
            val profilePictureUrl = userData["profilePictureUrl"] as? String

            binding.tvName.text = name
            binding.tvUsername.text = "@$username"
            binding.tvStreakCount.text = "$loginStreak Day Streak"
            binding.tvLongestStreak.text = "Longest streak: $longestStreak days"

            updateStreakBadge(loginStreak)
            updateStreakProgress(loginStreak)

            if (!profilePictureUrl.isNullOrEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(profilePictureUrl)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            } else {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder) // fallback image
            }

            val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            binding.cardProfile.startAnimation(fadeIn)

            // Handle login streak logic (update streak if needed)
            lastLoginTimestamp?.let { ts ->
                handleLoginStreak(userId, loginStreak, longestStreak, ts.seconds * 1000)
            }
        }
    }

    private fun updateStreakBadge(streak: Int) {
        val badgeRes = getStreakBadgeRes(streak)
        if (badgeRes != 0) {
            binding.ivStreakBadge.setImageResource(badgeRes)
            binding.ivStreakBadge.visibility = View.VISIBLE

            if (badgeRes != getStreakBadgeRes(lastKnownStreak)) {
                val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                binding.ivStreakBadge.startAnimation(animation)
                Toast.makeText(context, "New streak badge earned!", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.ivStreakBadge.visibility = View.GONE
        }
        lastKnownStreak = streak
    }

    private fun updateStreakProgress(streak: Int) {
        binding.circularProgressStreak.apply {
            progress = streak.toFloat()
            progressMax = 30f
            progressBarColor = resources.getColor(getProgressColor(streak), null)
            progressDirection = CircularProgressBar.ProgressDirection.TO_RIGHT
            progressBarWidth = resources.getDimension(R.dimen.progress_bar_width)
            backgroundProgressBarWidth = resources.getDimension(R.dimen.background_progress_bar_width)
            backgroundProgressBarColor = resources.getColor(R.color.progressBackground, null)
        }
        binding.tvStreakCount.text = "$streak Day Streak"
    }

    private fun getProgressColor(streak: Int): Int {
        return when {
            streak >= 20 -> R.color.progress_excellent
            streak >= 10 -> R.color.progress_good
            else -> R.color.progress_normal
        }
    }

    private suspend fun handleLoginStreak(userId: String, currentStreak: Int, longestStreak: Int, lastLoginMillis: Long) {
        val currentTime = System.currentTimeMillis()
        val daysBetween = getDaysBetween(lastLoginMillis, currentTime)

        val newStreak = when {
            daysBetween == 0L -> currentStreak
            daysBetween == 1L -> currentStreak + 1
            else -> 1
        }

        val updatedLongestStreak = maxOf(longestStreak, newStreak)

        // Update Firestore user document with new streak info
        val userRef = firestore.collection("users").document(userId)
        val updates = mapOf(
            "loginStreak" to newStreak,
            "lastLoginDate" to Timestamp.now(),
            "longestStreak" to updatedLongestStreak
        )

        try {
            userRef.update(updates).await()
            withContext(Dispatchers.Main) {
                binding.tvStreakCount.text = "$newStreak Day Streak"
                binding.tvLongestStreak.text = "Longest streak: $updatedLongestStreak days"
                updateStreakProgress(newStreak)
                updateStreakBadge(newStreak)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to update streak: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDaysBetween(oldDate: Long, newDate: Long): Long {
        val diff = newDate - oldDate
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    private fun getStreakBadgeRes(streak: Int): Int {
        return when {
            streak >= 20 -> R.drawable.badge_streak_20
            streak >= 15 -> R.drawable.badge_streak_15
            streak >= 9 -> R.drawable.badge_streak_9
            streak >= 7 -> R.drawable.badge_streak_7
            streak >= 5 -> R.drawable.badge_streak_5
            streak >= 3 -> R.drawable.badge_streak_3
            else -> 0
        }
    }

    private fun updateProfile(userId: String, newUsername: String) {
        lifecycleScope.launchWhenStarted {
            try {
                val userRef = firestore.collection("users").document(userId)
                val updates = mutableMapOf<String, Any>("username" to newUsername)

                // Upload profile picture if selected
                selectedImageUri?.let { uri ->
                    val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
                    val uploadTask = storageRef.putFile(uri).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    updates["profilePictureUrl"] = downloadUrl.toString()
                }

                userRef.update(updates).await()

                // Reload profile after update
                loadUserProfile(userId)

                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                selectedImageUri = null // reset selected image
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}