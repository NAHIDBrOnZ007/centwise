package com.centwise.core.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.centwise.R
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.TransactionType

/**
 * Home-screen widget showing this month's expense total.
 * Until the Rust database lands, it reads the shared repository state.
 */
class SpendingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val transactions = FakeTransactionRepository.shared.transactions.value

        val calendar = java.util.Calendar.getInstance()
        val monthlyExpense = transactions
            .filter { transaction ->
                if (transaction.type != TransactionType.EXPENSE) return@filter false
                val txCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = transaction.timestamp
                }
                txCalendar.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH) &&
                        txCalendar.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR)
            }
            .sumOf { it.amount }

        val monthlyIncome = transactions
            .filter { transaction ->
                if (transaction.type != TransactionType.INCOME) return@filter false
                val txCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = transaction.timestamp
                }
                txCalendar.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH) &&
                        txCalendar.get(java.util.Calendar.YEAR) == calendar.get(java.util.Calendar.YEAR)
            }
            .sumOf { it.amount }

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_spending).apply {
                setTextViewText(R.id.widget_spent_value, CurrencyFormatter.formatBDT(monthlyExpense))
                setTextViewText(R.id.widget_income_value, CurrencyFormatter.formatBDT(monthlyIncome))
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
