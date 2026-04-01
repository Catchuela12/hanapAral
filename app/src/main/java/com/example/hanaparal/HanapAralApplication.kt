package com.example.hanaparal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HanapAralApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    "hanaparal_general",
                    "General Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Used for general app notifications"
                },
                NotificationChannel(
                    "hanaparal_announcements",
                    "Group Announcements",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new group announcements"
                },
                NotificationChannel(
                    "hanaparal_reminders",
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for scheduled study sessions"
                }
            )

            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }
}
