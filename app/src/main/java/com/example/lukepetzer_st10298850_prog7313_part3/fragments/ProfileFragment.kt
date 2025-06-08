package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.User
import com.example.lukepetzer_st10298850_prog7313_part3.data.UserDao
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentProfileBinding
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.example.lukepetzer_st10298850_prog7313_part3.R
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem

import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var lastKnownStreak = 0
    private var selectedImageUri: Uri? = null

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the toolbar
        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        val userId = getUserIdFromSharedPreferences()
        if (userId != -1L) {
            loadUserProfile(userId)
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
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Achievements")
            .setMessage("🏅 Streak Badges:\n3+ days: Bronze\n5+ days: Silver\n10+ days: Gold\n20+ days: Diamond")
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etUsername)
        val btnChangePhoto = dialogView.findViewById<Button>(R.id.btnChangePhoto)

        etUsername.setText(binding.tvUsername.text.toString().removePrefix("@"))

        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newUsername = etUsername.text.toString()
                updateProfile(newUsername)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateProfile(newUsername: String) {
        lifecycleScope.launch {
            val userId = getUserIdFromSharedPreferences()
            val userDao = AppDatabase.getDatabase(requireContext()).userDao()

            // Update username
            userDao.updateUsername(userId, newUsername)

            // Update profile picture if a new one was selected
            selectedImageUri?.let { uri ->
                val imagePath = uri.toString()
                userDao.updateProfilePicture(userId, imagePath)
            }

            // Reload user profile
            loadUserProfile(userId)
        }
    }

    private fun getUserIdFromSharedPreferences(): Long {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getLong("USER_ID", -1)
    }

    private fun loadUserProfile(userId: Long) {
        lifecycleScope.launch {
            val userDao = AppDatabase.getDatabase(requireContext()).userDao()
            val user = userDao.getUserById(userId)
            user?.let {
                updateUI(it)
                handleLoginStreak(it, userDao)
            }
        }
    }

    private fun updateUI(user: User) {
        binding.tvName.text = user.name
        binding.tvUsername.text = "@${user.username}"
        binding.tvStreakCount.text = "${user.loginStreak} Day Streak"
        
        updateStreakBadge(user.loginStreak)
        updateStreakProgress(user.loginStreak)
        
        binding.tvLongestStreak.text = "Longest streak: ${user.longestStreak} days"
        
        // Load profile picture
        user.profilePicture?.let { picturePath ->
            Glide.with(this)
                .load(picturePath)
                .circleCrop()
                .into(binding.ivProfilePicture)
        }

        // Add fade-in animation for the entire profile card
        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        binding.cardProfile.startAnimation(fadeIn)
    }

    private fun updateStreakBadge(streak: Int) {
        val badgeRes = getStreakBadgeRes(streak)
        if (badgeRes != 0) {
            binding.ivStreakBadge.setImageResource(badgeRes)
            binding.ivStreakBadge.visibility = View.VISIBLE

            // Check if a new badge was earned
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

    private suspend fun handleLoginStreak(user: User, userDao: UserDao) {
        val currentTime = System.currentTimeMillis()
        val daysBetween = getDaysBetween(user.lastLoginDate, currentTime)

        val newStreak = when {
            daysBetween == 0L -> user.loginStreak  // Already logged in today
            daysBetween == 1L -> user.loginStreak + 1  // Continued streak
            else -> 1  // Missed a day, reset streak
        }

        val updatedLongestStreak = maxOf(user.longestStreak, newStreak)

        // Update user data
        userDao.updateLoginStreak(user.userId, newStreak, currentTime, updatedLongestStreak)

        // Update UI
        binding.tvStreakCount.text = "$newStreak Day Streak"
        binding.tvLongestStreak.text = "Longest streak: $updatedLongestStreak days"
        updateStreakProgress(newStreak)
        updateStreakBadge(newStreak)
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
            else -> 0 // No badge for streaks less than 3
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}