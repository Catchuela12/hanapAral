package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : ProfileRepository {

    override suspend fun saveProfile(user: User): Result<Unit> {
        return try {
            database.getReference("users")
                .child(user.uid)
                .setValue(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProfile(uid: String): Result<User> {
        return try {
            val snapshot = database.getReference("users")
                .child(uid)
                .get()
                .await()
            val user = snapshot.getValue(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            database.getReference("users")
                .child(uid)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProfile(uid: String): Flow<User?> = callbackFlow {
        val ref = database.getReference("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}