package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.adapters.TransactionAdapter
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentBudgetBinding
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.TransactionHistoryViewModel
import java.io.File
import java.util.Calendar
import java.util.Date

class BudgetFragment : Fragment() {
    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionHistoryViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    private var userId: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[TransactionHistoryViewModel::class.java]

        setupRecyclerView()
        setupFilterSpinner()
        observeTransactions()

        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)
        if (userId != -1L) {
            viewModel.loadAllTransactions(userId)
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { receiptPath ->
            showReceiptDialog(receiptPath)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All Time", "Last 7 Days", "Last 30 Days", "Custom Range")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = adapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.loadAllTransactions(userId)
                    1 -> viewModel.loadTransactionsForLastDays(userId, 7)
                    2 -> viewModel.loadTransactionsForLastDays(userId, 30)
                    3 -> showCustomDateRangePicker()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showCustomDateRangePicker() {
        val calendar = Calendar.getInstance()
        val startDatePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val startDate = calendar.time
                showEndDatePicker(calendar, startDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        startDatePicker.show()
    }

    private fun showEndDatePicker(startCalendar: Calendar, startDate: Date) {
        val endDatePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                val endDate = calendar.time
                viewModel.loadTransactions(userId, startDate, endDate)
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
        endDatePicker.datePicker.minDate = startCalendar.timeInMillis
        endDatePicker.show()
    }

    private fun observeTransactions() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            updateAvailableBalance(transactions)
        }
    }

    private fun updateAvailableBalance(transactions: List<Transaction>) {
        val balance = transactions.sumOf { transaction ->
            if (transaction.type == "Income") transaction.amount else -transaction.amount
        }
        binding.tvAvailableBalance.text = String.format("R%.2f", balance)
    }

    private fun showReceiptDialog(receiptUri: String) {
        Log.d("ReceiptDialog", "Loading image from: $receiptUri")

        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivReceipt)

        try {
            when {
                receiptUri.startsWith("content://") -> {
                    // It's a content URI
                    val uri = Uri.parse(receiptUri)
                    Glide.with(requireContext())
                        .load(uri)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imageView)
                }
                receiptUri.startsWith("file://") -> {
                    // It's a file URI
                    val uri = Uri.parse(receiptUri)
                    Glide.with(requireContext())
                        .load(uri)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imageView)
                }
                else -> {
                    // Assume it's a file path
                    val file = File(receiptUri)
                    Glide.with(requireContext())
                        .load(file)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imageView)
                }
            }
        } catch (e: Exception) {
            Log.e("ReceiptDialog", "Error loading image: ${e.message}", e)
            Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Receipt Preview")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}