package com.example.hanaparal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            val token = FirebaseMessaging.getInstance().token.await()
            database.getReference("fcm_tokens")
                .child(user.uid)
                .setValue(mapOf(
                    "token" to token,
                    "updatedAt" to System.currentTimeMillis()
                )).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        database.goOffline()
        auth.signOut()
        database.goOnline()
    }

    override suspend fun isProfileComplete(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val snapshot = database.getReference("users")
                .child(uid)
                .get()
                .await()
            snapshot.exists() && snapshot.child("name").value != null
        } catch (e: Exception) {
            false
        }
    }
}