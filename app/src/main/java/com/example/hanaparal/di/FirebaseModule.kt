package com.example.hanaparal.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirebaseDatabase() = FirebaseDatabase.getInstance()

    @Provides @Singleton
    fun provideFirebaseMessaging() = FirebaseMessaging.getInstance()

    @Provides @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance().apply {
            // Fetch every time during testing
            val settings = remoteConfigSettings { minimumFetchIntervalInSeconds = 0 }
            setConfigSettingsAsync(settings)
            setDefaultsAsync(mapOf("show_announcement" to false, "announcement_message" to ""))
        }
    }
}
