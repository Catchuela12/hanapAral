package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.User
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun saveProfile(user: User): Result<Unit>
    suspend fun getProfile(uid: String): Result<User>
    suspend fun updateProfile(uid: String, updates: Map<String, Any>): Result<Unit>
    fun observeProfile(uid: String): Flow<User?>
}