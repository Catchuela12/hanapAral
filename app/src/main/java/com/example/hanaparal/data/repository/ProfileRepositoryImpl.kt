package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    override suspend fun saveProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(uid: String): Result<User> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            val user = document.toObject(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProfile(uid: String): Flow<User?> {
        return firestore.collection("users")
            .document(uid)
            .snapshots()
            .map { it.toObject(User::class.java) }
    }
}