package com.example.hanaparal.data.repository

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.hanaparal.R
import com.example.hanaparal.data.source.DatabaseDataSource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepository @Inject constructor(
    private val databaseDataSource: DatabaseDataSource,
    private val messaging: FirebaseMessaging,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MessagingRepository"
    }

    suspend fun subscribeToGroup(groupId: String) {
        try {
            messaging.subscribeToTopic("group_$groupId").await()
            Log.d(TAG, "Subscribed to group_$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe: ${e.message}")
        }
    }

    suspend fun subscribeToAnnouncements(groupId: String) {
        try {
            messaging.subscribeToTopic("announcements_$groupId").await()
            Log.d(TAG, "Subscribed to announcements_$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe: ${e.message}")
        }
    }

    suspend fun sendNewMemberNotification(
        groupId: String,
        groupName: String,
        memberName: String
    ) {
        Log.d(TAG, "Simulating notification: $memberName joined $groupName")
        // In a real app, a backend would send this via FCM. 
        // For testing, we trigger a local notification to verify the UI/Channel works.
        showLocalNotification(
            title = "👋 New Member Joined!",
            body = "$memberName joined $groupName",
            channelId = "hanaparal_general"
        )
    }

    suspend fun sendAnnouncementNotification(
        groupId: String,
        groupName: String,
        senderName: String,
        message: String
    ) {
        Log.d(TAG, "Simulating announcement: $senderName in $groupName")
        showLocalNotification(
            title = "📢 $groupName",
            body = "$senderName: $message",
            channelId = "hanaparal_announcements"
        )
    }

    private fun showLocalNotification(title: String, body: String, channelId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    suspend fun unsubscribeFromGroup(groupId: String) = messaging.unsubscribeFromTopic("group_$groupId").await()
    suspend fun unsubscribeFromAnnouncements(groupId: String) = messaging.unsubscribeFromTopic("announcements_$groupId").await()
}
