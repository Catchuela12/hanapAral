package com.example.hanaparal.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : RemoteConfigRepository {

    override fun isGroupCreationEnabled(): Boolean {
        return remoteConfig.getBoolean("group_creation_enabled")
    }

    override fun getMaxMembers(): Int {
        return remoteConfig.getLong("max_members").toInt().takeIf { it > 0 } ?: 10
    }

    override fun getAnnouncementHeader(): String {
        return remoteConfig.getString("announcement_header")
    }

    override suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }
}
