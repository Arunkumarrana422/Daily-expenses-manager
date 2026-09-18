package com.example

import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.IncomeEntity
import com.example.utils.CurrencyFormatter
import com.example.utils.DateTimeUtils
import com.example.utils.ExportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testCurrencyFormatter() {
    val formattedInr = CurrencyFormatter.format(25450.0, "₹")
    assertEquals("₹25,450", formattedInr)

    val formattedUsd = CurrencyFormatter.format(1200.5, "$", showDecimals = true)
    assertEquals("$1,200.50", formattedUsd)
  }

  @Test
  fun testDateTimeUtils() {
    val todayStr = DateTimeUtils.getTodayString()
    assertTrue(todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

    val greeting = DateTimeUtils.getGreeting()
    assertTrue(greeting.isNotBlank())
  }

  @Test
  fun testExportUtilsCsv() {
    val expenses = listOf(
      ExpenseEntity(
        id = 1,
        amount = 500.0,
        categoryId = 1,
        categoryName = "Food",
        paymentMethod = "Cash",
        date = "2026-09-18",
        time = "12:30",
        note = "Lunch"
      )
    )
    val incomes = listOf(
      IncomeEntity(
        id = 1,
        amount = 5000.0,
        source = "Freelance",
        paymentMethod = "UPI",
        date = "2026-09-18",
        time = "10:00",
        note = "Design Project"
      )
    )

    val csv = ExportUtils.generateCsv(expenses, incomes)
    assertTrue(csv.contains("EXPENSE,2026-09-18,12:30,\"Food\",500.0,\"Cash\",\"Lunch\""))
    assertTrue(csv.contains("INCOME,2026-09-18,10:00,\"Freelance\",5000.0,\"UPI\",\"Design Project\""))
  }
}
