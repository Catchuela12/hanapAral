package com.example.hanaparal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            val token = FirebaseMessaging.getInstance().token.await()
            firestore.collection("fcm_tokens")
                .document(user.uid)
                .set(mapOf(
                    "token" to token,
                    "updatedAt" to System.currentTimeMillis()
                )).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun isProfileComplete(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            document.exists() && document.contains("name")
        } catch (e: Exception) {
            false
        }
    }
}