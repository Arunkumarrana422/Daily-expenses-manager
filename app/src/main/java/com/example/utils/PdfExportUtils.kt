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

    // App matching colors (Mint Green & Fresh Financial Palette)
    private val MintPrimary = Color.rgb(46, 204, 113)     // #2ECC71
    private val MintPrimaryDark = Color.rgb(39, 174, 96)  // #27AE60
    private val VibrantBlue = Color.rgb(52, 152, 219)     // #3498DB
    private val ExpenseRed = Color.rgb(231, 76, 60)       // #E74C3C
    private val IncomeGreen = Color.rgb(46, 204, 113)     // #2ECC71
    private val PageBackground = Color.rgb(244, 249, 246) // #F4F9F6
    private val SurfaceVariant = Color.rgb(232, 248, 240) // #E8F8F0
    private val TextPrimary = Color.rgb(27, 42, 34)       // #1B2A22
    private val TextSecondary = Color.rgb(90, 107, 98)    // #5A6B62
    private val GridLineColor = Color.rgb(220, 232, 225)  // #DCE8E1

    fun generateComprehensiveReportPdf(
        context: Context,
        title: String,
        period: String,
        totalIncome: Double,
        totalExpenses: Double,
        currency: String,
        categoryBreakdown: List<Triple<String, Double, Double>>,
        expenses: List<ExpenseEntity>,
        incomes: List<IncomeEntity>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 40f
        val rightMargin = 555f
        val bottomLimit = 780f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        fun drawBackground(c: Canvas) {
            val bgPaint = Paint().apply {
                isAntiAlias = true
                color = PageBackground
            }
            c.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
        }

        fun drawHeader(c: Canvas, pageNum: Int) {
            val headerBgPaint = Paint().apply {
                isAntiAlias = true
                color = MintPrimaryDark
            }
            c.drawRect(0f, 0f, pageWidth.toFloat(), if (pageNum == 1) 110f else 60f, headerBgPaint)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = if (pageNum == 1) 22f else 16f
                isFakeBoldText = true
            }
            c.drawText("DAILY EXPENSE MANAGER", 40f, if (pageNum == 1) 45f else 35f, titlePaint)

            if (pageNum == 1) {
                val subTitlePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(240, 255, 245)
                    textSize = 14f
                }
                c.drawText(title, 40f, 70f, subTitlePaint)

                val metaPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(210, 240, 225)
                    textSize = 10f
                }
                val currentDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                c.drawText("Period: $period  |  Generated: $currentDate", 40f, 92f, metaPaint)
            } else {
                val subTitlePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(240, 255, 245)
                    textSize = 11f
                }
                c.drawText("$title (Continued)", 40f, 50f, subTitlePaint)
            }
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = TextSecondary
                textSize = 10f
            }
            c.drawText("Daily Expense Manager • Page $pageNum", leftMargin, 815f, footerPaint)
        }

        drawBackground(canvas)
        drawHeader(canvas, pageNumber)

        var yPos = 135f

        if (pageNumber == 1) {
            // Summary Cards (Income, Expenses, Net)
            val cardWidth = (rightMargin - leftMargin - 20f) / 3f
            val cardHeight = 70f
            val net = totalIncome - totalExpenses

            fun drawMetricCard(x: Float, y: Float, lbl: String, value: String, valColor: Int, accentColor: Int) {
                val rect = RectF(x, y, x + cardWidth, y + cardHeight)
                canvas.drawRoundRect(rect, 8f, 8f, Paint().apply { isAntiAlias = true; color = Color.WHITE })
                canvas.drawRoundRect(rect, 8f, 8f, Paint().apply { isAntiAlias = true; color = GridLineColor; style = Paint.Style.STROKE; strokeWidth = 1f })
                canvas.drawRect(RectF(x, y, x + 6f, y + cardHeight), Paint().apply { isAntiAlias = true; color = accentColor })

                canvas.drawText(lbl.uppercase(), x + 16f, y + 22f, Paint().apply { isAntiAlias = true; color = TextSecondary; textSize = 10f; isFakeBoldText = true })
                canvas.drawText(value, x + 16f, y + 48f, Paint().apply { isAntiAlias = true; color = valColor; textSize = 14f; isFakeBoldText = true })
            }

            drawMetricCard(leftMargin, yPos, "Total Income", CurrencyFormatter.format(totalIncome, currency), IncomeGreen, MintPrimary)
            drawMetricCard(leftMargin + cardWidth + 10f, yPos, "Total Expenses", CurrencyFormatter.format(totalExpenses, currency), ExpenseRed, ExpenseRed)
            drawMetricCard(leftMargin + (cardWidth + 10f) * 2f, yPos, "Net Balance", CurrencyFormatter.format(net, currency), if (net >= 0) IncomeGreen else ExpenseRed, if (net >= 0) MintPrimary else ExpenseRed)

            yPos += cardHeight + 30f

            // Category Breakdown Section
            canvas.drawText("Category Breakdown", leftMargin, yPos, Paint().apply { isAntiAlias = true; color = TextPrimary; textSize = 15f; isFakeBoldText = true })
            yPos += 12f

            // Table header
            canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 26f), Paint().apply { isAntiAlias = true; color = SurfaceVariant })
            val thPaint = Paint().apply { isAntiAlias = true; color = TextPrimary; textSize = 11f; isFakeBoldText = true }
            canvas.drawText("CATEGORY", leftMargin + 12f, yPos + 17f, thPaint)
            canvas.drawText("AMOUNT", 340f, yPos + 17f, thPaint)
            canvas.drawText("PERCENTAGE", 460f, yPos + 17f, thPaint)
            yPos += 26f

            val rowPaint = Paint().apply { isAntiAlias = true; color = TextPrimary; textSize = 11f }
            val altRow = Paint().apply { isAntiAlias = true; color = Color.rgb(240, 252, 245) }
            val gridLine = Paint().apply { isAntiAlias = true; color = GridLineColor; strokeWidth = 1f }

            var idx = 0
            for (item in categoryBreakdown) {
                if (yPos > bottomLimit) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawBackground(canvas)
                    drawHeader(canvas, pageNumber)
                    yPos = 90f
                }

                if (idx % 2 == 1) canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 24f), altRow)
                canvas.drawText(item.first, leftMargin + 12f, yPos + 16f, rowPaint)
                canvas.drawText(CurrencyFormatter.format(item.second, currency), 340f, yPos + 16f, rowPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", item.third), 460f, yPos + 16f, rowPaint)
                yPos += 24f
                canvas.drawLine(leftMargin, yPos, rightMargin, yPos, gridLine)
                idx++
            }
            yPos += 20f
        }

        // Transactions List Section
        if (yPos > bottomLimit - 50f) {
            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            drawBackground(canvas)
            drawHeader(canvas, pageNumber)
            yPos = 90f
        }

        canvas.drawText("Transactions in this Period", leftMargin, yPos, Paint().apply { isAntiAlias = true; color = TextPrimary; textSize = 15f; isFakeBoldText = true })
        yPos += 12f

        // Tx Table Header
        canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 26f), Paint().apply { isAntiAlias = true; color = SurfaceVariant })
        val txTh = Paint().apply { isAntiAlias = true; color = TextPrimary; textSize = 11f; isFakeBoldText = true }
        canvas.drawText("TYPE", leftMargin + 12f, yPos + 17f, txTh)
        canvas.drawText("DATE & TIME", 120f, yPos + 17f, txTh)
        canvas.drawText("TITLE / CATEGORY", 260f, yPos + 17f, txTh)
        canvas.drawText("AMOUNT", 450f, yPos + 17f, txTh)
        yPos += 26f

        data class TxItem(val type: String, val date: String, val time: String, val title: String, val amount: Double, val isExp: Boolean)
        val allTx = mutableListOf<TxItem>()
        incomes.forEach { allTx.add(TxItem("INCOME", it.date, it.time, it.source, it.amount, false)) }
        expenses.forEach { allTx.add(TxItem("EXPENSE", it.date, it.time, it.categoryName + if(it.note.isNotBlank()) " (${it.note})" else "", it.amount, true)) }
        val sortedTx = allTx.sortedByDescending { "${it.date} ${it.time}" }

        val txRow = Paint().apply { isAntiAlias = true; textSize = 11f }
        val altRow = Paint().apply { isAntiAlias = true; color = Color.rgb(240, 252, 245) }
        val gridLine = Paint().apply { isAntiAlias = true; color = GridLineColor; strokeWidth = 1f }

        var txIdx = 0
        for (tx in sortedTx) {
            if (yPos > bottomLimit) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawBackground(canvas)
                drawHeader(canvas, pageNumber)
                yPos = 90f

                // Re-draw table header on new page
                canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 26f), Paint().apply { isAntiAlias = true; color = SurfaceVariant })
                canvas.drawText("TYPE", leftMargin + 12f, yPos + 17f, txTh)
                canvas.drawText("DATE & TIME", 120f, yPos + 17f, txTh)
                canvas.drawText("TITLE / CATEGORY", 260f, yPos + 17f, txTh)
                canvas.drawText("AMOUNT", 450f, yPos + 17f, txTh)
                yPos += 26f
            }

            if (txIdx % 2 == 1) canvas.drawRect(RectF(leftMargin, yPos, rightMargin, yPos + 24f), altRow)

            txRow.isFakeBoldText = true
            txRow.color = if (tx.isExp) ExpenseRed else IncomeGreen
            canvas.drawText(tx.type, leftMargin + 12f, yPos + 16f, txRow)

            txRow.isFakeBoldText = false
            txRow.color = TextSecondary
            canvas.drawText("${tx.date}  ${tx.time}", 120f, yPos + 16f, txRow)

            txRow.color = TextPrimary
            val titleText = if (tx.title.length > 28) tx.title.substring(0, 25) + "..." else tx.title
            canvas.drawText(titleText, 260f, yPos + 16f, txRow)

            txRow.isFakeBoldText = true
            txRow.color = if (tx.isExp) ExpenseRed else IncomeGreen
            val amtStr = (if (tx.isExp) "- " else "+ ") + CurrencyFormatter.format(tx.amount, currency)
            canvas.drawText(amtStr, 450f, yPos + 16f, txRow)

            yPos += 24f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, gridLine)
            txIdx++
        }

        drawFooter(canvas, pageNumber)
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
        return generateComprehensiveReportPdf(
            context,
            "Complete Transaction History",
            "All Time",
            incomes.sumOf { it.amount },
            expenses.sumOf { it.amount },
            currency,
            emptyList(),
            expenses,
            incomes
        )
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
