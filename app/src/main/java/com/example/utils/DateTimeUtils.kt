package com.example.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val fullDisplayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    private val shortDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun getTodayString(): String = LocalDate.now().format(dateFormatter)
    fun getCurrentTimeString(): String = LocalTime.now().format(timeFormatter)

    fun getFormattedToday(): String = LocalDate.now().format(fullDisplayFormatter)

    fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 4..11 -> "Good Morning 👋"
            in 12..16 -> "Good Afternoon ☀️"
            in 17..21 -> "Good Evening 🌆"
            else -> "Good Night 🌙"
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val today = LocalDate.now()
            when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(shortDisplayFormatter)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun isDateInCurrentMonth(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val now = LocalDate.now()
            date.year == now.year && date.month == now.month
        } catch (e: Exception) {
            false
        }
    }

    fun isDateInCurrentWeek(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val now = LocalDate.now()
            val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)
            val weekEnd = weekStart.plusDays(6)
            !date.isBefore(weekStart) && !date.isAfter(weekEnd)
        } catch (e: Exception) {
            false
        }
    }
}
