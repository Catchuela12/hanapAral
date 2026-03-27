package com.example.hanaparal.data.source

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingDataSource @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val database: FirebaseDatabase
) {
    suspend fun getCurrentToken(): String {
        return messaging.token.await()
    }

    suspend fun saveTokenToDatabase(uid: String) {
        val token = getCurrentToken()
        database.getReference("fcm_tokens")
            .child(uid)
            .setValue(mapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )).await()
    }

    suspend fun deleteToken() {
        messaging.deleteToken().await()
    }

    suspend fun subscribeToGroup(groupId: String) {
        messaging.subscribeToTopic("group_$groupId").await()
    }

    suspend fun unsubscribeFromGroup(groupId: String) {
        messaging.unsubscribeFromTopic("group_$groupId").await()
    }

    suspend fun subscribeToAnnouncements(groupId: String) {
        messaging.subscribeToTopic("announcements_$groupId").await()
    }

    suspend fun unsubscribeFromAnnouncements(groupId: String) {
        messaging.unsubscribeFromTopic("announcements_$groupId").await()
    }
}