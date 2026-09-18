package com.example.utils

import android.content.Context
import android.content.Intent
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity

object ExportUtils {

    fun generateCsv(
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("Type,Date,Time,Category/Source,Amount,Payment Method,Note\n")

        for (inc in incomes) {
            sb.append("INCOME,")
                .append(inc.date).append(",")
                .append(inc.time).append(",")
                .append("\"").append(inc.source.replace("\"", "\"\"")).append("\",")
                .append(inc.amount).append(",")
                .append("\"").append(inc.paymentMethod).append("\",")
                .append("\"").append(inc.note.replace("\"", "\"\"")).append("\"\n")
        }

        for (exp in expenses) {
            sb.append("EXPENSE,")
                .append(exp.date).append(",")
                .append(exp.time).append(",")
                .append("\"").append(exp.categoryName.replace("\"", "\"\"")).append("\",")
                .append(exp.amount).append(",")
                .append("\"").append(exp.paymentMethod).append("\",")
                .append("\"").append(exp.note.replace("\"", "\"\"")).append("\"\n")
        }

        return sb.toString()
    }

    fun shareReport(
        context: Context,
        reportTitle: String,
        content: String
    ) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, reportTitle)
            putExtra(Intent.EXTRA_SUBJECT, reportTitle)
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Financial Report")
        context.startActivity(shareIntent)
    }
}
