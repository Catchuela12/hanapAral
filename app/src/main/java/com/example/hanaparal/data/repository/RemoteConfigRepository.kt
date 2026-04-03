package com.example.hanaparal.data.repository

interface RemoteConfigRepository {
    fun isGroupCreationEnabled(): Boolean
    fun getMaxMembers(): Int
    fun getAnnouncementHeader(): String
    suspend fun fetchAndActivate(): Boolean
}
