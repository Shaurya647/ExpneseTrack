package com.example.expensetrack

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetrack.adapter.ExpenseAdapter
import com.example.expensetrack.database.AppDatabase
import com.example.expensetrack.database.Expense
import com.example.expensetrack.databinding.ActivityCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: ExpenseAdapter
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()

        binding.tvBack.setOnClickListener { finish() }

        // Initialize with today's date
        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)

        // Set CalendarView listener
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            loadExpensesForDate(year, month, dayOfMonth)
        }

        loadExpensesForDate(selectedYear, selectedMonth, selectedDay)
    }

    override fun onResume() {
        super.onResume()
        loadExpensesForDate(selectedYear, selectedMonth, selectedDay)
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(emptyList()) { expense ->
            startActivity(
                Intent(this, ExpenseDetailActivity::class.java).apply {
                    putExtra("EXPENSE_ID", expense.id)
                }
            )
        }
        binding.recyclerViewCalendarExpenses.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity)
            adapter = this@CalendarActivity.adapter
        }
    }

    private fun loadExpensesForDate(year: Int, month: Int, dayOfMonth: Int) {
        val datePrimary = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)

        lifecycleScope.launch(Dispatchers.IO) {
            val allExpenses = database.expenseDao().getAllExpenses()
            val expenses = allExpenses.filter { matchesDate(it.date, year, month, dayOfMonth) }
            val totalSpent = expenses.sumOf { it.amount }

            withContext(Dispatchers.Main) {
                binding.apply {
                    tvSelectedDateLabel.text = "EXPENSES FOR $datePrimary"
                    tvDayTotal.text = Utils.formatCurrency(totalSpent)
                    tvDayExpenseCount.text = if (expenses.size == 1) "1 expense" else "${expenses.size} expenses"

                    if (expenses.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        recyclerViewCalendarExpenses.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        recyclerViewCalendarExpenses.visibility = View.VISIBLE
                        adapter.updateList(expenses)
                    }
                }
            }
        }
    }

    private fun matchesDate(expenseDate: String, targetYear: Int, targetMonth: Int, targetDay: Int): Boolean {
        val cleanDate = expenseDate.trim()
        if (cleanDate.isEmpty()) return false

        val formattedTarget1 = String.format(Locale.getDefault(), "%02d/%02d/%04d", targetDay, targetMonth + 1, targetYear)
        val formattedTarget2 = String.format(Locale.getDefault(), "%d/%d/%04d", targetDay, targetMonth + 1, targetYear)
        val formattedTarget3 = String.format(Locale.getDefault(), "%d/%02d/%04d", targetDay, targetMonth + 1, targetYear)
        val formattedTarget4 = String.format(Locale.getDefault(), "%02d/%d/%04d", targetDay, targetMonth + 1, targetYear)

        if (cleanDate.contains(formattedTarget1) ||
            cleanDate.contains(formattedTarget2) ||
            cleanDate.contains(formattedTarget3) ||
            cleanDate.contains(formattedTarget4)) {
            return true
        }

        val dateFormats = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd-MM-yyyy",
            "d-M-yyyy",
            "yyyy-MM-dd",
            "yyyy-M-d",
            "dd MMM yyyy",
            "d MMM yyyy",
            "MM/dd/yyyy",
            "M/d/yyyy"
        )

        for (format in dateFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                val parsedDate = sdf.parse(cleanDate)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance().apply { time = parsedDate }
                    if (cal.get(Calendar.YEAR) == targetYear &&
                        cal.get(Calendar.MONTH) == targetMonth &&
                        cal.get(Calendar.DAY_OF_MONTH) == targetDay) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Continue checking next format
            }
        }
        return false
    }
}
