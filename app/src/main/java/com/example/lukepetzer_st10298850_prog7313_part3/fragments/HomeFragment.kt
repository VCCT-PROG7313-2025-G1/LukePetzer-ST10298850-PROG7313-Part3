package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentHomeBinding
import com.example.lukepetzer_st10298850_prog7313_part3.utils.ChartUtils
import com.example.lukepetzer_st10298850_prog7313_part3.utils.toCurrency
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            AppDatabase.getDatabase(requireContext()).transactionDao(),
            AppDatabase.getDatabase(requireContext()).categoryDao(),
            AppDatabase.getDatabase(requireContext()).budgetGoalDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = getUserId()
        viewModel.loadUserData(userId)
        viewModel.loadBudgetGoal(userId)
        setupDateFilterSpinner()
        setupUI()
        bindObservers()
    }

    private fun setupUI() {
        binding.currentDateText.text = viewModel.getCurrentDateFormatted()
        binding.setGoalsButton.setOnClickListener {
            showSetGoalsDialog()
        }
        // setupSearchView()
    }

    private fun bindObservers() {
        with(viewModel) {
            totalSpending.observe(viewLifecycleOwner) {
                binding.tvTotalSpending.text = it.toCurrency()
            }
            remainingBudget.observe(viewLifecycleOwner) {
                binding.tvRemainingBudget.text = "${it.toCurrency()} left to spend today"
            }
            budgetGoals.observe(viewLifecycleOwner) { (assigned, remaining) ->
                val progress = if (assigned + remaining > 0) (assigned / (assigned + remaining) * 100).toInt() else 0
                binding.progressBudgetGoals.progress = progress
                binding.tvBudgetGoalsDetails.text = "${assigned.toCurrency()} assigned / ${remaining.toCurrency()} remaining"
            }
            categoryTotals.observe(viewLifecycleOwner) {
                updateCharts(it)
            }
            categoryProgress.observe(viewLifecycleOwner) {
                updateCategoryProgressBars(it)
            }
            transactions.observe(viewLifecycleOwner) {
                updateDateRangeDisplay()
                updateChartsFromTransactions(it)
            }
            categories.observe(viewLifecycleOwner) { categoryList ->
                val categories = listOf("All Categories") + categoryList
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCategory.adapter = adapter

                binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selected = if (position == 0) null else categories[position]
                        viewModel.setCategory(selected)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            budgetGoalData.observe(viewLifecycleOwner) { goal ->
                goal?.let {
                    binding.tvBudgetGoalsDetails.text = "Short: ${it.shortTermGoal.toCurrency()}, Max: ${it.maxGoal.toCurrency()}"
                }
            }
        }
    }

    private fun updateCharts(categoryTotals: Map<String, Double>) {
        ChartUtils.configurePieChart(binding.pieChart, categoryTotals)
    }

    private fun updateChartsFromTransactions(transactions: List<Transaction>) {
        val totals = transactions.groupBy { it.category }
            .mapValues { it.value.sumOf { txn -> txn.amount } }
        updateCharts(totals)
    }

    private fun updateCategoryProgressBars(data: List<CategoryProgress>) {
        binding.categoryProgressContainer.removeAllViews()
        data.forEach { item ->
            val view = layoutInflater.inflate(R.layout.item_category_progress, binding.categoryProgressContainer, false)
            view.findViewById<TextView>(R.id.tvCategoryName).text = item.category
            view.findViewById<TextView>(R.id.tvAmount).text = item.spent.toCurrency()
            val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
            progressBar.progress = item.progress
            progressBar.progressDrawable.setColorFilter(getProgressColor(item.progress), android.graphics.PorterDuff.Mode.SRC_IN)
            binding.categoryProgressContainer.addView(view)
        }
    }

    private fun getProgressColor(progress: Int): Int {
        return when {
            progress < 50 -> ContextCompat.getColor(requireContext(), R.color.progress_green)
            progress < 80 -> ContextCompat.getColor(requireContext(), R.color.progress_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.progress_red)
        }
    }

    private fun updateDateRangeDisplay() {
        val text = when (viewModel.getCurrentDateFilter()) {
            HomeViewModel.DateFilter.DAILY -> "Today"
            HomeViewModel.DateFilter.WEEKLY -> "This Week"
            HomeViewModel.DateFilter.MONTHLY -> "This Month"
            HomeViewModel.DateFilter.CUSTOM -> {
                val format = SimpleDateFormat("dd MMM", Locale.getDefault())
                val start = viewModel.getCustomStartDate()?.let { format.format(it) } ?: ""
                val end = viewModel.getCustomEndDate()?.let { format.format(it) } ?: ""
                "$start – $end"
            }
        }
        binding.tvDateRange.text = text
    }

    private fun setupDateFilterSpinner() {
        val options = listOf("Daily", "Weekly", "Monthly", "Custom")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDateFilter.adapter = adapter

        binding.spinnerDateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        viewModel.setDateFilter(HomeViewModel.DateFilter.DAILY)
                        updateDateRangeDisplay()
                    }
                    1 -> {
                        viewModel.setDateFilter(HomeViewModel.DateFilter.WEEKLY)
                        updateDateRangeDisplay()
                    }
                    2 -> {
                        viewModel.setDateFilter(HomeViewModel.DateFilter.MONTHLY)
                        updateDateRangeDisplay()
                    }
                    3 -> {
                        showCustomDatePickerDialog()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showCustomDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val (year, month, day) = arrayOf(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        DatePickerDialog(requireContext(), { _, startYear, startMonth, startDay ->
            val start = Calendar.getInstance().apply { set(startYear, startMonth, startDay) }

            DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                val end = Calendar.getInstance().apply { set(endYear, endMonth, endDay) }
                viewModel.setCustomDateRange(start.time, end.time)
                updateDateRangeDisplay()
            }, year, month, day).apply {
                datePicker.minDate = start.timeInMillis
            }.show()
        }, year, month, day).show()
    }

    private fun showSetGoalsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_goals, null)
        val shortTermInput = dialogView.findViewById<EditText>(R.id.etShortTermGoal)
        val maxGoalInput = dialogView.findViewById<EditText>(R.id.etMaxGoal)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Budget Goals")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val shortTerm = shortTermInput.text.toString().toDoubleOrNull() ?: 0.0
                val maxGoal = maxGoalInput.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.saveBudgetGoal(getUserId(), shortTerm, maxGoal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Uncomment and implement if you want to add search functionality
    // private fun setupSearchView() {
    //     binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
    //         override fun onQueryTextSubmit(query: String?): Boolean {
    //             query?.let { viewModel.searchTransactions(it) }
    //             return true
    //         }
    //
    //         override fun onQueryTextChange(newText: String?): Boolean {
    //             // Optional: Live search
    //             return true
    //         }
    //     })
    // }

    private fun getUserId(): Long {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getLong("USER_ID", -1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}