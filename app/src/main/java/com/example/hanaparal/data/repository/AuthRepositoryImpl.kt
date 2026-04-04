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
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Sign in failed: User is null")

            // Update FCM token on sign-in
            try {
                val token = messaging.token.await()
                firestore.collection("fcm_tokens")
                    .document(user.uid)
                    .set(mapOf(
                        "token" to token,
                        "updatedAt" to System.currentTimeMillis()
                    )).await()
            } catch (e: Exception) {
                // Log token failure but don't fail the entire sign-in
                e.printStackTrace()
            }

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
            
            // Check if the user document exists and has a name set
            document.exists() && !document.getString("name").isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
}
