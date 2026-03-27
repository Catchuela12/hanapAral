package com.example.hanaparal.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepository @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) {
    companion object {
        const val KEY_GROUP_CREATION_ENABLED = "group_creation_enabled"
        const val KEY_MAX_MEMBERS = "max_members_per_group"
        const val KEY_ANNOUNCEMENT_HEADER = "announcement_header"
        const val KEY_SUPERUSER_EMAIL = "superuser_email"
    }

    suspend fun fetchAndActivate(): Result<Unit> {
        return try {
            val settings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
            }
            remoteConfig.setConfigSettingsAsync(settings).await()
            remoteConfig.setDefaultsAsync(
                mapOf(
                    KEY_GROUP_CREATION_ENABLED to true,
                    KEY_MAX_MEMBERS to 10L,
                    KEY_ANNOUNCEMENT_HEADER to "Welcome to HanapAral",
                    KEY_SUPERUSER_EMAIL to ""
                )
            ).await()
            remoteConfig.fetchAndActivate().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isGroupCreationEnabled(): Boolean =
        remoteConfig.getBoolean(KEY_GROUP_CREATION_ENABLED)

    fun getMaxMembers(): Int =
        remoteConfig.getLong(KEY_MAX_MEMBERS).toInt()

    fun getAnnouncementHeader(): String =
        remoteConfig.getString(KEY_ANNOUNCEMENT_HEADER)

    fun getSuperuserEmail(): String =
        remoteConfig.getString(KEY_SUPERUSER_EMAIL)
}