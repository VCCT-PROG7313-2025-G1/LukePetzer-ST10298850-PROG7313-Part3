package com.example.lukepetzer_st10298850_prog7313_part3.fragments

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
import com.google.firebase.firestore.FirebaseFirestore
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentHomeBinding
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.*
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.HomeViewModel
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.HomeViewModelFactory
import com.example.lukepetzer_st10298850_prog7313_part3.data.CategoryProgress
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.utils.toCurrency
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        setupUI()
        bindObservers()

        viewModel.loadMonthlySpending(userId)
    }

    private fun setupUI() {
        binding.currentDateText.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        binding.setGoalsButton.setOnClickListener { showSetGoalsDialog() }
    }

    private fun bindObservers() {
        viewModel.apply {
            transactions.observe(viewLifecycleOwner) { tx ->
                updateChartsFromTransactions(tx)
            }

            monthlyExpenses.observe(viewLifecycleOwner) { expense ->
                binding.tvTotalSpending.text = expense.toCurrency()
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
                    val shortTermAssigned = it.shortTermGoal
                    val longTermAssigned = it.maxGoal
                    val total = it.maxGoal

                    val shortTermProgress = if (total > 0) ((shortTermAssigned / total) * 100).toInt() else 0
                    binding.progressShortTermGoal.progress = shortTermProgress

                    val longTermProgress = if (total > 0) ((longTermAssigned / total) * 100).toInt() else 0
                    binding.progressLongTermGoal.progress = longTermProgress

                    binding.tvShortTermGoalDetails.text = "Short-Term: ${shortTermAssigned.toCurrency()}"
                    binding.tvLongTermGoalDetails.text = "Long-Term: ${longTermAssigned.toCurrency()}"
                } ?: run {
                    binding.progressShortTermGoal.progress = 0
                    binding.progressLongTermGoal.progress = 0
                    binding.tvShortTermGoalDetails.text = ""
                    binding.tvLongTermGoalDetails.text = ""
                }
            }

            remainingBudget.observe(viewLifecycleOwner) { rem ->
                binding.tvRemainingBudget.text = "${rem.toCurrency()} left to spend today"
            }

            categoryTotals.observe(viewLifecycleOwner) { totals ->
                if (totals.isNotEmpty()) {
                    updateCategoryBreakdown(totals)
                } else {
                    binding.barChart.setNoDataText("No data available")
                    binding.barChart.invalidate()
                }
            }

            categoryProgress.observe(viewLifecycleOwner) { bars -> updateCategoryProgressBars(bars) }
        }
    }

    private fun updateCategoryBreakdown(breakdown: Map<String, Double>) {
        val entries = breakdown.entries.mapIndexed { index, (category, amount) ->
            BarEntry(index.toFloat(), amount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Category Breakdown")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val barData = BarData(dataSet)
        binding.barChart.data = barData

        val xAxis = binding.barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(breakdown.keys.toList())
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        binding.barChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun updateChartsFromTransactions(transactions: List<Transaction>) {
        val totals = transactions.groupBy { it.category }.mapValues { it.value.sumOf { txn -> txn.amount } }
        updateCategoryBreakdown(totals)
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