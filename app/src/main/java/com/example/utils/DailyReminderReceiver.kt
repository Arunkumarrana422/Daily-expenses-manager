package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("DailyReminderReceiver", "Alarm received with action: $action")

        val preferences = UserPreferencesDataStore(context.applicationContext)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPrefs = preferences.preferencesFlow.first()
                if (userPrefs.dailyReminderEnabled) {
                    if (action == "com.example.action.DAILY_REMINDER") {
                        NotificationHelper.showNotification(
                            context = context.applicationContext,
                            title = "Daily Expense Reminder 🌙",
                            message = "It's 9:00 PM! Don't forget to log your daily expenses and check your budget."
                        )
                    }
                    // Reschedule for next day 9:00 PM (or after boot)
                    NotificationHelper.scheduleDailyEveningReminder(context.applicationContext)
                }
            } catch (e: Exception) {
                Log.e("DailyReminderReceiver", "Error processing reminder alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
