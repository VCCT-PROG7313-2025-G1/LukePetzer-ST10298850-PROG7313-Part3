package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentHomeBinding
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.*
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.HomeViewModel
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.HomeViewModelFactory
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.utils.ChartUtils
import com.example.lukepetzer_st10298850_prog7313_part3.utils.toCurrency
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
//        _binding = inflater.inflate(R.layout.fragment_home, container, false)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase-backed repositories
        val transactionRepo = TransactionRepository(FirebaseFirestore.getInstance())
        val categoryRepo = CategoryRepository()
        val budgetGoalRepo = BudgetGoalRepository(FirebaseFirestore.getInstance())

        // Use ViewModel Factory to enable DI
        val factory = HomeViewModelFactory(transactionRepo, categoryRepo, budgetGoalRepo)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Check authenticated user
        val userId = getUserSession()
        if (userId.isNullOrEmpty()) {
            findNavController().navigate(R.id.action_global_to_login)
            return
        }

        // Load initial data
        viewModel.setUserId(userId)

        setupDateFilterSpinner()
        setupUI()
        bindObservers()
    }

    private fun setupUI() {
        binding.currentDateText.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        binding.setGoalsButton.setOnClickListener { showSetGoalsDialog() }
    }

    private fun bindObservers() {
        viewModel.apply {
            transactions.observe(viewLifecycleOwner) { tx ->
                updateDateRangeDisplay()
                updateChartsFromTransactions(tx)
                val total = tx.sumOf { it.amount }
                binding.tvTotalSpending.text = total.toCurrency()
            }

            categories.observe(viewLifecycleOwner) { cats ->
                val list = listOf("All Categories") + cats.map { it.name }
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, list).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCategory.adapter = adapter
                }
                binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        setCategory(if (pos == 0) null else list[pos])
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            budgetGoal.observe(viewLifecycleOwner) { goal ->
                goal?.let {
                    val assigned = it.shortTermGoal
                    val remaining = it.maxGoal - assigned
                    val total = it.maxGoal
                    val progress = if (total > 0) ((assigned / total) * 100).toInt() else 0
                    binding.progressBudgetGoals.progress = progress
                    binding.tvBudgetGoalsDetails.text = "${assigned.toCurrency()} assigned / ${remaining.toCurrency()} remaining"
                } ?: run {
                    binding.progressBudgetGoals.progress = 0
                    binding.tvBudgetGoalsDetails.text = "No budget goal set"
                }
            }

            remainingBudget.observe(viewLifecycleOwner) { rem ->
                binding.tvRemainingBudget.text = "${rem.toCurrency()} left to spend today"
            }

            categoryTotals.observe(viewLifecycleOwner) { totals ->
                if (totals.isNotEmpty()) {
                    updateCharts(totals)
                } else {
                    // Handle empty totals
                    binding.pieChart.setNoDataText("No data available")
                    binding.pieChart.invalidate()
                }
            }

            categoryProgress.observe(viewLifecycleOwner) { bars -> updateCategoryProgressBars(bars) }
        }
    }

    private fun updateCharts(categoryTotals: Map<String, Double>) {
        ChartUtils.configurePieChart(binding.pieChart, categoryTotals)
    }

    private fun updateChartsFromTransactions(transactions: List<Transaction>) {
        val totals = transactions.groupBy { it.category }.mapValues { it.value.sumOf { txn -> txn.amount } }
        updateCharts(totals)
    }

    private fun updateCategoryProgressBars(data: List<CategoryProgress>) {
        binding.categoryProgressContainer.removeAllViews()
        data.forEach { item ->
            val view = layoutInflater.inflate(R.layout.item_category_progress, binding.categoryProgressContainer, false)
            view.findViewById<TextView>(R.id.tvCategoryName).text = item.category
            view.findViewById<TextView>(R.id.tvAmount).text = item.spent.toCurrency()
            val pb = view.findViewById<ProgressBar>(R.id.progressBar)
            pb.progress = item.progress
            pb.progressDrawable.setColorFilter(getProgressColor(item.progress), android.graphics.PorterDuff.Mode.SRC_IN)
            binding.categoryProgressContainer.addView(view)
        }
    }

    private fun getProgressColor(p: Int): Int = when {
        p < 50 -> ContextCompat.getColor(requireContext(), R.color.progress_green)
        p < 80 -> ContextCompat.getColor(requireContext(), R.color.progress_yellow)
        else -> ContextCompat.getColor(requireContext(), R.color.progress_red)
    }

    private fun updateDateRangeDisplay() {
        binding.tvDateRange.text = when (viewModel.currentFilter) {
            HomeViewModel.DateFilter.DAILY -> "Today"
            HomeViewModel.DateFilter.WEEKLY -> "This Week"
            HomeViewModel.DateFilter.MONTHLY -> "This Month"
            HomeViewModel.DateFilter.CUSTOM -> viewModel.customRangeFormatted()
        }
    }

    private fun setupDateFilterSpinner() {
        val opts = listOf("Daily", "Weekly", "Monthly", "Custom")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opts).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDateFilter.adapter = it
        }
        binding.spinnerDateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos == 3) return showCustomDatePickerDialog()
                viewModel.setDateFilter(HomeViewModel.DateFilter.values()[pos])
                updateDateRangeDisplay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showCustomDatePickerDialog() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val start = Calendar.getInstance().apply { set(y, m, d) }.time
            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                viewModel.setCustomDateRange(start, Calendar.getInstance().apply { set(y2, m2, d2) }.time)
                updateDateRangeDisplay()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = start.time }.show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showSetGoalsDialog() {
        val dialog = layoutInflater.inflate(R.layout.dialog_set_goals, null)
        val shortInput = dialog.findViewById<EditText>(R.id.etShortTermGoal)
        val maxInput = dialog.findViewById<EditText>(R.id.etMaxGoal)
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", "")

        AlertDialog.Builder(requireContext())
            .setTitle("Set Goals")
            .setView(dialog)
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveBudgetGoal(
                    userId.toString(),
                    shortInput.text.toString().toDoubleOrNull() ?: 0.0,
                    maxInput.text.toString().toDoubleOrNull() ?: 0.0
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUserSession(): String? {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", null)
        Log.d("LoginFragment", "Retrieved user session: $userId")
        return if (userId.isNullOrEmpty()) null else userId
    }
}