package com.example.lukepetzer_st10298850_prog7313_part3.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

object ChartUtils {
    fun configureBarChart(barChart: BarChart, categoryTotals: Map<String, Double>) {
        val entries = categoryTotals.entries.mapIndexed { index, (_, total) ->
            BarEntry(index.toFloat(), total.toFloat())
        }
        val dataSet = BarDataSet(entries, "Category Totals")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val barData = BarData(dataSet)

        barChart.apply {
            data = barData
            setFitBars(true)
            description.isEnabled = false
            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.setDrawInside(true)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(categoryTotals.keys.toList())
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false
            setDrawGridBackground(false)
            animateY(1000)
            invalidate()
        }
    }

    fun configurePieChart(pieChart: PieChart, categoryTotals: Map<String, Double>) {
        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }
        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter())
        pieData.setValueTextSize(11f)
        pieData.setValueTextColor(Color.WHITE)

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Category\nBreakdown"
            setCenterTextSize(16f)
            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.setDrawInside(false)
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            animateY(1000)
            invalidate()
        }
    }
}