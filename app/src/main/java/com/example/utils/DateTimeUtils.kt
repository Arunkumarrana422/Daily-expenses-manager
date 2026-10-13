package com.example.utils

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val fullDisplayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    private val shortDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun getTodayString(): String = LocalDate.now().format(dateFormatter)
    fun getCurrentTimeString(): String = LocalTime.now().format(timeFormatter12)

    fun formatDisplayTime(timeStr: String): String {
        if (timeStr.isBlank()) return ""
        val normalized = timeStr.trim()
        if (normalized.contains("AM", ignoreCase = true) || normalized.contains("PM", ignoreCase = true)) {
            // Already 12-hour formatted, standardize it
            return try {
                val cleaned = normalized.replace(".", "").uppercase(Locale.US)
                val parsed = LocalTime.parse(cleaned, DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
                parsed.format(timeFormatter12)
            } catch (_: Exception) {
                normalized
            }
        }
        return try {
            val parsed24 = LocalTime.parse(normalized, timeFormatter24)
            parsed24.format(timeFormatter12)
        } catch (_: Exception) {
            try {
                val parsed = LocalTime.parse(normalized)
                parsed.format(timeFormatter12)
            } catch (_: Exception) {
                normalized
            }
        }
    }

    /**
     * Converts any time string (e.g. "10:43 am", "10:43 PM", "22:43", "9:05 AM")
     * into total minutes from start of day (0 to 1439) for correct chronological sorting.
     */
    fun parseTimeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val raw = timeStr.trim().replace(".", "")
        // Try parsing as 12-hour format with AM/PM (e.g., "10:43 AM", "1:15 pm", "08:30AM")
        val amPmMatch = Regex("(?i)^(\\d{1,2}):(\\d{2})\\s*(AM|PM)$").find(raw)
        if (amPmMatch != null) {
            val (hStr, mStr, meridiem) = amPmMatch.destructured
            var hour = hStr.toIntOrNull() ?: 0
            val minute = mStr.toIntOrNull() ?: 0
            val isPm = meridiem.equals("PM", ignoreCase = true)
            if (isPm) {
                if (hour < 12) hour += 12
            } else {
                if (hour == 12) hour = 0
            }
            return hour * 60 + minute
        }

        // Try parsing as 24-hour format (e.g., "22:43", "09:15")
        val h24Match = Regex("^(\\d{1,2}):(\\d{2})").find(raw)
        if (h24Match != null) {
            val (hStr, mStr) = h24Match.destructured
            val hour = hStr.toIntOrNull() ?: 0
            val minute = mStr.toIntOrNull() ?: 0
            return hour * 60 + minute
        }

        return 0
    }

    fun getFormattedToday(): String = LocalDate.now().format(fullDisplayFormatter)

    fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 4..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
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

    fun isDateInPreviousMonth(dateStr: String): Boolean {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            val prevMonth = LocalDate.now().minusMonths(1)
            date.year == prevMonth.year && date.month == prevMonth.month
        } catch (e: Exception) {
            false
        }
    }

    fun isDateInMonth(dateStr: String, year: Int, month: Int): Boolean {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.year == year && date.monthValue == month
        } catch (e: Exception) {
            false
        }
    }

    fun getPreviousMonthDisplay(): String {
        val prev = LocalDate.now().minusMonths(1)
        return prev.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    fun getPreviousMonthShortName(): String {
        val prev = LocalDate.now().minusMonths(1)
        return prev.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
    }

    fun getCurrentMonthDisplay(): String {
        val now = LocalDate.now()
        return now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
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
