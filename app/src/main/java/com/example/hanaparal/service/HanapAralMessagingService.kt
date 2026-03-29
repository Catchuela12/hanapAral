package com.example.hanaparal.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.hanaparal.MainActivity
import com.example.hanaparal.R
import com.example.hanaparal.data.source.MessagingDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HanapAralMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var messagingDataSource: MessagingDataSource

    @Inject
    lateinit var auth: FirebaseAuth

    companion object {
        const val CHANNEL_ID_GENERAL = "hanaparal_general"
        const val CHANNEL_ID_ANNOUNCEMENTS = "hanaparal_announcements"
        const val CHANNEL_ID_REMINDERS = "hanaparal_reminders"
        const val CHANNEL_NAME_GENERAL = "General Notifications"
        const val CHANNEL_NAME_ANNOUNCEMENTS = "Group Announcements"
        const val CHANNEL_NAME_REMINDERS = "Study Reminders"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        auth.currentUser?.uid?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    messagingDataSource.saveTokenToDatabase(uid)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        createNotificationChannels()

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "HanapAral"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new notification"

        val type = message.data["type"] ?: "general"

        when (type) {
            "new_member" -> showNotification(
                title = title,
                body = body,
                channelId = CHANNEL_ID_GENERAL,
                notificationId = System.currentTimeMillis().toInt()
            )
            "announcement" -> showNotification(
                title = title,
                body = body,
                channelId = CHANNEL_ID_ANNOUNCEMENTS,
                notificationId = System.currentTimeMillis().toInt()
            )
            "reminder" -> showNotification(
                title = title,
                body = body,
                channelId = CHANNEL_ID_REMINDERS,
                notificationId = System.currentTimeMillis().toInt()
            )
            else -> showNotification(
                title = title,
                body = body,
                channelId = CHANNEL_ID_GENERAL,
                notificationId = System.currentTimeMillis().toInt()
            )
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        notificationId: Int
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // General channel
            NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                description = "Notifications for new members joining groups"
            }.also { notificationManager.createNotificationChannel(it) }

            // Announcements channel
            NotificationChannel(
                CHANNEL_ID_ANNOUNCEMENTS,
                CHANNEL_NAME_ANNOUNCEMENTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                description = "Notifications for group announcements"
            }.also { notificationManager.createNotificationChannel(it) }

            // Reminders channel
            NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                description = "Study session reminders"
            }.also { notificationManager.createNotificationChannel(it) }
        }
    }
}