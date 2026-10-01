package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(250, 250, 252)
        }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 1. Header Banner Background (Deep Indigo)
        val headerBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(63, 81, 181) // Indigo Primary
        }
        canvas.drawRect(0f, 0f, 595f, 110f, headerBgPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("DAILY EXPENSE MANAGER", 40f, 45f, titlePaint)

        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(224, 224, 224)
            textSize = 14f
        }
        canvas.drawText(title, 40f, 70f, subTitlePaint)

        val metaPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(200, 200, 200)
            textSize = 10f
        }
        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Period: $period  |  Generated: $currentDate", 40f, 92f, metaPaint)

        var yPos = 135f
        val leftMargin = 40f
        val rightMargin = 555f

        // 2. Summary Cards Section (3 cards: Income, Expenses, Net Balance)
        val cardWidth = (rightMargin - leftMargin - 20f) / 3f
        val cardHeight = 70f
        val net = totalIncome - totalExpenses

        fun drawMetricCard(x: Float, y: Float, label: String, value: String, valueColor: Int, accentBarColor: Int) {
            val cardRect = RectF(x, y, x + cardWidth, y + cardHeight)
            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cardRect, 8f, 8f, cardPaint)

            val borderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(230, 232, 238)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

            val accentRect = RectF(x, y, x + 6f, y + cardHeight)
            val accentPaint = Paint().apply {
                isAntiAlias = true
                color = accentBarColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(accentRect, accentPaint)

            val lblPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(117, 117, 117)
                textSize = 10f
                isFakeBoldText = true
            }
            canvas.drawText(label.uppercase(), x + 16f, y + 22f, lblPaint)

            val valPaint = Paint().apply {
                isAntiAlias = true
                color = valueColor
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText(value, x + 16f, y + 48f, valPaint)
        }

        drawMetricCard(leftMargin, yPos, "Total Income", CurrencyFormatter.format(totalIncome, currency), Color.rgb(56, 142, 60), Color.rgb(76, 175, 80))
        drawMetricCard(leftMargin + cardWidth + 10f, yPos, "Total Expenses", CurrencyFormatter.format(totalExpenses, currency), Color.rgb(211, 47, 47), Color.rgb(244, 67, 54))
        drawMetricCard(leftMargin + (cardWidth + 10f) * 2f, yPos, "Net Balance", CurrencyFormatter.format(net, currency), if (net >= 0) Color.rgb(56, 142, 60) else Color.rgb(211, 47, 47), if (net >= 0) Color.rgb(76, 175, 80) else Color.rgb(244, 67, 54))

        yPos += cardHeight + 35f

        // 3. Category Breakdown Header
        val sectionTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("Category Breakdown", leftMargin, yPos, sectionTitlePaint)
        yPos += 15f

        val tableHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(238, 240, 245)
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 28f), tableHeaderPaint)

        val tableHeaderTxt = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(80, 80, 80)
            textSize = 11f
            isFakeBoldText = true
        }
        canvas.drawText("CATEGORY", leftMargin + 12f, yPos + 18f, tableHeaderTxt)
        canvas.drawText("AMOUNT", 340f, yPos + 18f, tableHeaderTxt)
        canvas.drawText("PERCENTAGE", 460f, yPos + 18f, tableHeaderTxt)

        yPos += 28f

        val rowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 12f
        }
        val altRowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }
        val gridLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(230, 232, 238)
            strokeWidth = 1f
        }

        var index = 0
        for (item in categoryBreakdown) {
            if (yPos > 760f) break

            if (index % 2 == 1) {
                canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 26f), altRowPaint)
            }

            canvas.drawText(item.first, leftMargin + 12f, yPos + 17f, rowPaint)
            canvas.drawText(CurrencyFormatter.format(item.second, currency), 340f, yPos + 17f, rowPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", item.third), 460f, yPos + 17f, rowPaint)

            yPos += 26f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, gridLinePaint)
            index++
        }

        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(150, 150, 150)
            textSize = 10f
        }
        canvas.drawText("Daily Expense Manager • Financial Summary Report", leftMargin, 815f, footerPaint)

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

        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(250, 250, 252)
        }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val headerBgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(63, 81, 181)
        }
        canvas.drawRect(0f, 0f, 595f, 100f, headerBgPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("TRANSACTION HISTORY", 40f, 45f, titlePaint)

        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(224, 224, 224)
            textSize = 13f
        }
        val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Complete Income & Expense Records  |  Generated: $currentDate", 40f, 72f, subTitlePaint)

        var yPos = 130f
        val leftMargin = 40f
        val rightMargin = 555f

        val tableHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(238, 240, 245)
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 28f), tableHeaderPaint)

        val tableHeaderTxt = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(80, 80, 80)
            textSize = 11f
            isFakeBoldText = true
        }
        canvas.drawText("TYPE", leftMargin + 12f, yPos + 18f, tableHeaderTxt)
        canvas.drawText("DATE & TIME", 120f, yPos + 18f, tableHeaderTxt)
        canvas.drawText("TITLE / CATEGORY", 260f, yPos + 18f, tableHeaderTxt)
        canvas.drawText("AMOUNT", 450f, yPos + 18f, tableHeaderTxt)

        yPos += 28f

        data class TxItem(val type: String, val date: String, val time: String, val title: String, val amount: Double, val isExp: Boolean)
        val list = mutableListOf<TxItem>()
        incomes.forEach { list.add(TxItem("INCOME", it.date, it.time, it.source, it.amount, false)) }
        expenses.forEach { list.add(TxItem("EXPENSE", it.date, it.time, it.categoryName + if(it.note.isNotBlank()) " (${it.note})" else "", it.amount, true)) }

        val rowPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
        }
        val altRowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }
        val gridLinePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(230, 232, 238)
            strokeWidth = 1f
        }

        var index = 0
        for (tx in list.sortedByDescending { "${it.date} ${it.time}" }) {
            if (yPos > 770f) break

            if (index % 2 == 1) {
                canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 26f), altRowPaint)
            }

            rowPaint.isFakeBoldText = true
            rowPaint.color = if (tx.isExp) Color.rgb(211, 47, 47) else Color.rgb(56, 142, 60)
            canvas.drawText(tx.type, leftMargin + 12f, yPos + 17f, rowPaint)

            rowPaint.isFakeBoldText = false
            rowPaint.color = Color.rgb(80, 80, 80)
            canvas.drawText("${tx.date}  ${tx.time}", 120f, yPos + 17f, rowPaint)

            rowPaint.color = Color.rgb(33, 33, 33)
            val truncatedTitle = if (tx.title.length > 28) tx.title.substring(0, 25) + "..." else tx.title
            canvas.drawText(truncatedTitle, 260f, yPos + 17f, rowPaint)

            rowPaint.isFakeBoldText = true
            rowPaint.color = if (tx.isExp) Color.rgb(211, 47, 47) else Color.rgb(56, 142, 60)
            val amtStr = (if (tx.isExp) "- " else "+ ") + CurrencyFormatter.format(tx.amount, currency)
            canvas.drawText(amtStr, 450f, yPos + 17f, rowPaint)

            yPos += 26f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, gridLinePaint)
            index++
        }

        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(150, 150, 150)
            textSize = 10f
        }
        canvas.drawText("Daily Expense Manager • Transaction History Report", leftMargin, 815f, footerPaint)

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
