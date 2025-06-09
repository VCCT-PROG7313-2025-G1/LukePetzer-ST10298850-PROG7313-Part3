package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val GITHUB_README_URL = "https://github.com/LukePetzer/LukePetzerST10298850PROG7313Part3/blob/main/README.md"

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Retrieve userId from SharedPreferences
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        userId = sharedPref.getString("USER_ID", null)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Navigate to Transaction Categories
        binding.llTransactionCategories.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_transactionCategoriesFragment)
        }

        // Toggle Notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement notification toggle logic
            // For example, you could save the preference to SharedPreferences
            val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("NOTIFICATIONS_ENABLED", isChecked)
                apply()
            }
            Toast.makeText(context, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        // Help & Support
        binding.llHelpSupport.setOnClickListener {
            openUrl(GITHUB_README_URL)
        }

        // Sign Out
        binding.btnSignOut.setOnClickListener {
            signOut()
        }

        // Export User Data
        binding.llExportData.setOnClickListener {
            exportUserData()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun exportUserData() {
        if (userId == null) {
            Toast.makeText(context, "User not logged in. Please log in and try again.", Toast.LENGTH_LONG).show()
            // Navigate to login screen
            findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val userData = getUserData(userId!!)
                val transactions = getTransactions(userId!!)
                val categories = getCategories(userId!!)
                val budgetGoals = getBudgetGoals(userId!!)

                val exportData = buildExportString(userData, transactions, categories, budgetGoals)
                saveToFile(exportData)

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun getUserData(userId: String): Map<String, Any> {
        return firestore.collection("users").document(userId).get().await().data ?: emptyMap()
    }

    private suspend fun getTransactions(userId: String): List<Map<String, Any>> {
        return firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.data }
    }

    private suspend fun getCategories(userId: String): List<Map<String, Any>> {
        return firestore.collection("categories")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.data }
    }

    private suspend fun getBudgetGoals(userId: String): Map<String, Any> {
        return firestore.collection("budgetGoals").document(userId).get().await().data ?: emptyMap()
    }

    private fun buildExportString(
        userData: Map<String, Any>,
        transactions: List<Map<String, Any>>,
        categories: List<Map<String, Any>>,
        budgetGoals: Map<String, Any>
    ): String {
        return buildString {
            appendLine("User Data:")
            appendLine(userData.toString())
            appendLine("\nTransactions:")
            transactions.forEach { appendLine(it.toString()) }
            appendLine("\nCategories:")
            categories.forEach { appendLine(it.toString()) }
            appendLine("\nBudget Goals:")
            appendLine(budgetGoals.toString())
        }
    }

    private fun saveToFile(data: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "user_data_export_$timestamp.txt"
        val file = File(requireContext().getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            writer.write(data)
        }
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Clear user session from SharedPreferences
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("USER_ID")
            apply()
        }

        // Clear the local userId
        userId = null

        // Show a toast message
        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to the login page
        findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}