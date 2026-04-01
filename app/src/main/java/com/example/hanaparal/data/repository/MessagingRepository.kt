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
import android.util.Log
import com.example.hanaparal.data.source.DatabaseDataSource
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private val messaging: FirebaseMessaging
) {
    companion object {
        private const val TAG = "MessagingRepository"
        private const val PROJECT_ID = "hanaparal-a5ffd"
        private const val FCM_URL =
            "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
    }

    // ── Topic Subscriptions ────────────────────────────────────────────────────

    suspend fun subscribeToGroup(groupId: String) {
        try {
            messaging.subscribeToTopic("group_$groupId").await()
            Log.d(TAG, "Subscribed to group_$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe: ${e.message}")
            Log.e(TAG, "Failed to subscribe to group: ${e.message}")
        }
    }

    suspend fun unsubscribeFromGroup(groupId: String) {
        try {
            messaging.unsubscribeFromTopic("group_$groupId").await()
            Log.d(TAG, "Unsubscribed from group_$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from group: ${e.message}")
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

            Log.e(TAG, "Failed to subscribe to announcements: ${e.message}")
        }
    }

    suspend fun unsubscribeFromAnnouncements(groupId: String) {
        try {
            messaging.unsubscribeFromTopic("announcements_$groupId").await()
            Log.d(TAG, "Unsubscribed from announcements_$groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from announcements: ${e.message}")
        }
    }

    // ── Send Notifications ─────────────────────────────────────────────────────

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
    ) = withContext(Dispatchers.IO) {
        try {
            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("topic", "group_$groupId")
                    put("notification", JSONObject().apply {
                        put("title", "👋 New Member Joined!")
                        put("body", "$memberName joined $groupName")
                    })
                    put("data", JSONObject().apply {
                        put("type", "new_member")
                        put("groupId", groupId)
                        put("groupName", groupName)
                        put("memberName", memberName)
                    })
                    put("android", JSONObject().apply {
                        put("notification", JSONObject().apply {
                            put("channel_id", "hanaparal_general")
                            put("sound", "default")
                            put("priority", "high")
                        })
                    })
                })
            }
            Log.d(TAG, "New member notification prepared for topic: group_$groupId")
            Log.d(TAG, "Message: ${message.toString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send new member notification: ${e.message}")
        }
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
    ) = withContext(Dispatchers.IO) {
        try {
            val fcmMessage = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("topic", "announcements_$groupId")
                    put("notification", JSONObject().apply {
                        put("title", "📢 $groupName")
                        put("body", "$senderName: $message")
                    })
                    put("data", JSONObject().apply {
                        put("type", "announcement")
                        put("groupId", groupId)
                        put("groupName", groupName)
                        put("senderName", senderName)
                    })
                    put("android", JSONObject().apply {
                        put("notification", JSONObject().apply {
                            put("channel_id", "hanaparal_announcements")
                            put("sound", "default")
                            put("priority", "high")
                        })
                    })
                })
            }
            Log.d(TAG, "Announcement notification prepared for topic: announcements_$groupId")
            Log.d(TAG, "Message: ${fcmMessage.toString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send announcement notification: ${e.message}")
        }
    }

    suspend fun sendStudyReminder(
        groupId: String,
        groupName: String,
        schedule: String
    ) = withContext(Dispatchers.IO) {
        try {
            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("topic", "group_$groupId")
                    put("notification", JSONObject().apply {
                        put("title", "📚 Study Reminder!")
                        put("body", "$groupName meets $schedule")
                    })
                    put("data", JSONObject().apply {
                        put("type", "reminder")
                        put("groupId", groupId)
                        put("groupName", groupName)
                        put("schedule", schedule)
                    })
                    put("android", JSONObject().apply {
                        put("notification", JSONObject().apply {
                            put("channel_id", "hanaparal_reminders")
                            put("sound", "default")
                            put("priority", "high")
                        })
                    })
                })
            }
            Log.d(TAG, "Study reminder prepared for topic: group_$groupId")
            Log.d(TAG, "Message: ${message.toString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send study reminder: ${e.message}")
        }
    }
}
