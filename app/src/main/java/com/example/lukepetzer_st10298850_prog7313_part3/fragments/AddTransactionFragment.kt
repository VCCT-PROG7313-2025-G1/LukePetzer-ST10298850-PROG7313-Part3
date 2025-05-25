package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentAddTransactionBinding
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.AddTransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var viewModel: AddTransactionViewModel
    private var currentUserId: Long = -1
    private var categories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(this)[AddTransactionViewModel::class.java]

        // Retrieve the user ID from SharedPreferences
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("USER_ID", -1)

        if (currentUserId == -1L) {
            Toast.makeText(context, "Please log in to add a transaction", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_addTransactionFragment_to_loginFragment)
            return
        }

        setupViews()
        setupListeners()
        observeCategories()
        viewModel.loadCategories(currentUserId)
    }

    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->
            categories = categoryList
            val categoryNames = categoryList.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }
    }

    private fun setupViews() {
        // Set default date to today
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(Date()))
    }

    private fun setupListeners() {
        binding.btnAddTransaction.setOnClickListener {
            addTransaction()
        }

        binding.btnAddReceipt.setOnClickListener {
            // TODO: Implement add receipt logic
            Toast.makeText(context, "Add receipt functionality to be implemented", Toast.LENGTH_SHORT).show()
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun addTransaction() {
        val type =
            if (binding.toggleGroup.checkedButtonId == R.id.btnIncome) "Income" else "Expense"
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val categoryPosition = binding.spinnerCategory.selectedItemPosition
        val category = categories.getOrNull(categoryPosition)?.name ?: return
        val date = binding.etDate.text.toString()
        val description = binding.etDescription.text.toString()

        if (amount == null) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            userId = currentUserId,
            type = type,
            amount = amount,
            category = category,
            date = date,
            description = description.ifEmpty { null },
            receiptUri = null // TODO: Implement receipt handling
        )

        lifecycleScope.launch {
            try {
                database.transactionDao().insertTransaction(transaction)
                Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()

                val navOptions = NavOptions.Builder()
                    .setPopUpTo(
                        R.id.navigation_add,
                        true
                    ) // This will remove the AddTransactionFragment from the back stack
                    .build()
                findNavController().navigate(R.id.navigation_home, null, navOptions)
            } catch (e: Exception) {
                Toast.makeText(context, "Error adding transaction: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

        private fun showDatePicker() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etDate.setText(dateFormat.format(selectedDate.time))
            }, year, month, day).show()
        }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}