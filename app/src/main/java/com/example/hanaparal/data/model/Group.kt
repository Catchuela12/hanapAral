package com.example.hanaparal.data.model

data class Group(
    val groupId: String = "",
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val schedule: String = "",
    val creatorId: String = "",
    val adminId: String = "",
    val memberCount: Int = 0,
    val maxMembers: Int = 10,
    val createdAt: Long = 0L
)