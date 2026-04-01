package com.example.hanaparal.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user!!
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUid(): String? = auth.currentUser?.uid

    fun getCurrentEmail(): String? = auth.currentUser?.email

    fun getCurrentDisplayName(): String? = auth.currentUser?.displayName

    fun getCurrentPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()
}