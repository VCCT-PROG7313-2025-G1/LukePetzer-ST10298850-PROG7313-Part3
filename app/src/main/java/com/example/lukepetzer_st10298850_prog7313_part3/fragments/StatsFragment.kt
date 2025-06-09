package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentStatsBinding
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.StatsViewModel
import android.content.res.ColorStateList
import com.example.lukepetzer_st10298850_prog7313_part3.utils.ChartMarkerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getLong("USER_ID", -1)

        if (userId != -1L) {
            viewModel.monthlySummary.observe(viewLifecycleOwner) { summary ->
                binding.tvTotalIncome.text = formatCurrency(summary.income)
                binding.tvTotalExpenses.text = formatCurrency(summary.expenses)
                binding.tvRemainingBudget.text = formatCurrency(summary.remaining)
            }

            viewModel.budgetUsage.observe(viewLifecycleOwner) { usage ->
                binding.pbBudgetUsage.progress = usage.percentUsed.toInt()
                binding.tvBudgetUsedLabel.text = "${usage.percentUsed.toInt()}% of budget used"
                updateProgressBarColor(usage.percentUsed)
            }

            viewModel.monthlySpending.observe(viewLifecycleOwner) { entries ->
                setupLineChart(entries)
            }

            viewModel.loadMonthlySummary(userId.toString())
            viewModel.loadBudgetUsage(userId.toString())
            viewModel.loadMonthlySpending(userId.toString())
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "R%.2f".format(amount)
    }

    private fun updateProgressBarColor(percentUsed: Double) {
        val color = when {
            percentUsed >= 80 -> ContextCompat.getColor(requireContext(), R.color.progress_red)
            percentUsed >= 60 -> ContextCompat.getColor(requireContext(), R.color.progress_yellow)
            else -> ContextCompat.getColor(requireContext(), R.color.progress_green)
        }
        binding.pbBudgetUsage.progressTintList = ColorStateList.valueOf(color)
    }

    private fun setupLineChart(entries: List<Entry>) {
        val lineChart = binding.lineChart

        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No spending data available.")
            return
        }

        lineChart.apply {
            setBackgroundColor(Color.WHITE)
            setDrawGridBackground(false)
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.form = Legend.LegendForm.LINE
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_color_primary)
        }

        // X Axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_color_primary)
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setAvoidFirstLastClipping(true)

        // Format X-axis labels as dates
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        xAxis.valueFormatter = IndexAxisValueFormatter(entries.mapIndexed { index, _ ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, index)
            dateFormatter.format(calendar.time)
        })

        // Y Axis
        val yAxis = lineChart.axisLeft
        yAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_color_primary)
        yAxis.setDrawGridLines(true)
        yAxis.gridColor = ContextCompat.getColor(requireContext(), R.color.grid_color)
        yAxis.axisMinimum = 0f
        yAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R%.0f".format(value)
            }
        }

        // DataSet
        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            color = ContextCompat.getColor(requireContext(), R.color.progress_primary)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_color_primary)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.accent))
            setDrawCircles(true)
            circleRadius = 4f
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill_gradient)
            setDrawHighlightIndicators(true)
            highLightColor = Color.MAGENTA
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "R%.2f".format(value)
                }
            }
        }

        // Marker for data points
        val markerView = ChartMarkerView(requireContext(), R.layout.chart_marker_view)
        markerView.chartView = lineChart
        lineChart.marker = markerView

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
        lineChart.animateXY(1000, 1000)

        // Optional: Debug
        Log.d("LineChart", "Entries count: ${entries.size}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}