package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import java.io.File
import java.io.FileOutputStream

object PdfExportUtils {

    fun generateFinancialReportPdf(
        context: Context,
        title: String,
        period: String,
        totalIncome: Double,
        totalExpenses: Double,
        currency: String,
        categoryBreakdown: List<Triple<String, Double, Double>>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(92, 107, 192)
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var yPos = 50f
        val leftMargin = 40f

        canvas.drawText("Daily Expense Manager", leftMargin, yPos, titlePaint)
        yPos += 25f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText(title, leftMargin, yPos, paint)
        yPos += 20f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.GRAY
        canvas.drawText("Period: $period", leftMargin, yPos, paint)
        yPos += 30f

        paint.color = Color.BLACK
        canvas.drawLine(leftMargin, yPos, 555f, yPos, linePaint)
        yPos += 25f

        canvas.drawText("Summary Overview", leftMargin, yPos, headerPaint)
        yPos += 25f

        canvas.drawText("Total Income: ${CurrencyFormatter.format(totalIncome, currency)}", leftMargin, yPos, paint)
        yPos += 20f
        canvas.drawText("Total Expenses: ${CurrencyFormatter.format(totalExpenses, currency)}", leftMargin, yPos, paint)
        yPos += 20f
        val net = totalIncome - totalExpenses
        paint.isFakeBoldText = true
        paint.color = if (net >= 0) Color.rgb(76, 175, 80) else Color.rgb(211, 47, 47)
        canvas.drawText("Net Balance: ${CurrencyFormatter.format(net, currency)}", leftMargin, yPos, paint)
        paint.isFakeBoldText = false
        paint.color = Color.BLACK
        yPos += 40f

        canvas.drawText("Category Breakdown", leftMargin, yPos, headerPaint)
        yPos += 25f

        canvas.drawText("Category", leftMargin, yPos, paint)
        canvas.drawText("Amount", 300f, yPos, paint)
        canvas.drawText("Percentage", 450f, yPos, paint)
        yPos += 10f
        canvas.drawLine(leftMargin, yPos, 555f, yPos, linePaint)
        yPos += 20f

        for (item in categoryBreakdown) {
            if (yPos > 780f) break
            canvas.drawText(item.first, leftMargin, yPos, paint)
            canvas.drawText(CurrencyFormatter.format(item.second, currency), 300f, yPos, paint)
            canvas.drawText(String.format("%.1f%%", item.third), 450f, yPos, paint)
            yPos += 22f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Financial_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
        pdfDocument.close()
        return file
    }

    fun generateTransactionsPdf(
        context: Context,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>,
        currency: String
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(92, 107, 192)
            textSize = 18f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var yPos = 40f
        val leftMargin = 30f

        canvas.drawText("Transaction History Report", leftMargin, yPos, titlePaint)
        yPos += 30f

        canvas.drawText("Type", leftMargin, yPos, headerPaint)
        canvas.drawText("Date & Time", 100f, yPos, headerPaint)
        canvas.drawText("Title / Category", 230f, yPos, headerPaint)
        canvas.drawText("Amount", 420f, yPos, headerPaint)
        yPos += 8f
        canvas.drawLine(leftMargin, yPos, 565f, yPos, linePaint)
        yPos += 18f

        data class TxItem(val type: String, val date: String, val time: String, val title: String, val amount: Double, val isExp: Boolean)
        val list = mutableListOf<TxItem>()
        incomes.forEach { list.add(TxItem("INCOME", it.date, it.time, it.source, it.amount, false)) }
        expenses.forEach { list.add(TxItem("EXPENSE", it.date, it.time, it.categoryName + if(it.note.isNotBlank()) " (${it.note})" else "", it.amount, true)) }
        
        for (tx in list.sortedByDescending { "${it.date} ${it.time}" }) {
            if (yPos > 800f) break
            paint.color = if (tx.isExp) Color.rgb(211, 47, 47) else Color.rgb(76, 175, 80)
            canvas.drawText(tx.type, leftMargin, yPos, paint)
            
            paint.color = Color.BLACK
            canvas.drawText("${tx.date} ${tx.time}", 100f, yPos, paint)
            
            val truncatedTitle = if (tx.title.length > 25) tx.title.substring(0, 22) + "..." else tx.title
            canvas.drawText(truncatedTitle, 230f, yPos, paint)
            
            paint.color = if (tx.isExp) Color.rgb(211, 47, 47) else Color.rgb(76, 175, 80)
            val amtStr = (if (tx.isExp) "-" else "+") + CurrencyFormatter.format(tx.amount, currency)
            canvas.drawText(amtStr, 420f, yPos, paint)
            
            paint.color = Color.BLACK
            yPos += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Transactions_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
        pdfDocument.close()
        return file
    }

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title)
        context.startActivity(chooser)
    }
}
