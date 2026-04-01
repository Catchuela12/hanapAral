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
            val manager = getSystemService(NotificationManager::class.java)
            val channels = listOf(
                NotificationChannel("hanaparal_general", "General", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("hanaparal_announcements", "Announcements", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("hanaparal_reminders", "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
            manager.createNotificationChannel(channels[0])
            manager.createNotificationChannel(channels[1])
            manager.createNotificationChannel(channels[2])
        }
    }
}
