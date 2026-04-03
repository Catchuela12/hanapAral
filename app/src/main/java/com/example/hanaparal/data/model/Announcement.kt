package com.example.hanaparal.data.model

data class Announcement(
    val announcementId: String = "",
    val groupId: String = "",
    val message: String = "",
    val sentBy: String = "",
    val senderName: String = "",
    val createdAt: Long = 0L
)