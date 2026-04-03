package com.example.hanaparal.data.repository

interface MessagingRepository {
    suspend fun subscribeToGroup(groupId: String)
    suspend fun unsubscribeFromGroup(groupId: String)
    suspend fun subscribeToAnnouncements(groupId: String)
    suspend fun unsubscribeFromAnnouncements(groupId: String)
    suspend fun sendNewMemberNotification(groupId: String, groupName: String, memberName: String)
    suspend fun sendStudyReminder(groupId: String, groupName: String, schedule: String)
    suspend fun sendAnnouncementNotification(
        groupId: String,
        groupName: String,
        senderName: String,
        message: String
    )
}
