package com.example.expensetrack

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.databinding.ActivityAnalysisBinding
import com.example.expensetrack.databinding.ItemAnalysisLegendBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        binding.tvBack.setOnClickListener { finish() }

        loadAnalysisData()
    }

    private fun loadAnalysisData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val expenses = database.expenseDao().getAllExpenses()
            val totalExpense = expenses.sumOf { it.amount }

            val categories = resources.getStringArray(R.array.categories)
            val categorySlices = mutableListOf<PieChartView.PieSlice>()

            categories.forEach { category ->
                val categoryAmount = expenses.filter { it.category == category }.sumOf { it.amount }
                val color = getCategoryColor(category)
                if (categoryAmount > 0) {
                    categorySlices.add(PieChartView.PieSlice(category, categoryAmount, color))
                }
            }

            // Sort by amount descending
            categorySlices.sortByDescending { it.value }

            withContext(Dispatchers.Main) {
                binding.pieChartView.setData(categorySlices, animate = true)

                binding.legendContainer.removeAllViews()
                if (categorySlices.isEmpty()) {
                    val emptyTv = android.widget.TextView(this@AnalysisActivity).apply {
                        text = "No expenses recorded yet."
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        textSize = 14f
                        setPadding(0, 32, 0, 32)
                    }
                    binding.legendContainer.addView(emptyTv)
                } else {
                    categorySlices.forEach { slice ->
                        val legendBinding = ItemAnalysisLegendBinding.inflate(
                            LayoutInflater.from(this@AnalysisActivity),
                            binding.legendContainer,
                            false
                        )

                        val dotDrawable = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(slice.color)
                        }
                        legendBinding.viewColorDot.background = dotDrawable

                        legendBinding.tvCategoryName.text = slice.category
                        val percentage = if (totalExpense > 0) (slice.value / totalExpense) * 100 else 0.0
                        legendBinding.tvPercentage.text = String.format(Locale.getDefault(), "%.1f%% of total", percentage)
                        legendBinding.tvCategoryAmount.text = Utils.formatCurrency(slice.value)

                        binding.legendContainer.addView(legendBinding.root)
                    }
                }
            }
        }
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Food" -> ContextCompat.getColor(this, R.color.food_icon)
            "Travel" -> ContextCompat.getColor(this, R.color.travel_icon)
            "Shopping" -> ContextCompat.getColor(this, R.color.shopping_icon)
            "Bills" -> ContextCompat.getColor(this, R.color.bills_icon)
            "Entertainment" -> ContextCompat.getColor(this, R.color.entertainment_icon)
            else -> ContextCompat.getColor(this, R.color.other_icon)
        }
    }
}
