package com.example.hanaparal.data.source

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
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
class DatabaseDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {

    // ── Users ──────────────────────────────────────────────────────────────────

    suspend fun saveUser(user: User) {
        database.getReference("users")
            .child(user.uid)
            .setValue(user)
            .await()
    }

    suspend fun getUser(uid: String): User? {
        val snapshot = database.getReference("users")
            .child(uid)
            .get()
            .await()
        return snapshot.getValue(User::class.java)
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        database.getReference("users")
            .child(uid)
            .updateChildren(updates)
            .await()
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val ref = database.getReference("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Groups ─────────────────────────────────────────────────────────────────

    suspend fun createGroup(group: Group): String {
        val ref = database.getReference("groups").push()
        val groupId = ref.key!!
        ref.setValue(group.copy(groupId = groupId)).await()
        return groupId
    }

    suspend fun getGroup(groupId: String): Group? {
        val snapshot = database.getReference("groups")
            .child(groupId)
            .get()
            .await()
        return snapshot.getValue(Group::class.java)
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>) {
        database.getReference("groups")
            .child(groupId)
            .updateChildren(updates)
            .await()
    }

    fun observeAllGroups(): Flow<List<Group>> = callbackFlow {
        val ref = database.getReference("groups")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull {
                    it.getValue(Group::class.java)
                }
                trySend(groups)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val ref = database.getReference("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Group::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Members ────────────────────────────────────────────────────────────────

    suspend fun addMember(groupId: String, member: Member) {
        database.getReference("members")
            .child(groupId)
            .child(member.uid)
            .setValue(member)
            .await()
    }

    suspend fun removeMember(groupId: String, uid: String) {
        database.getReference("members")
            .child(groupId)
            .child(uid)
            .removeValue()
            .await()
    }

    suspend fun isMember(groupId: String, uid: String): Boolean {
        val snapshot = database.getReference("members")
            .child(groupId)
            .child(uid)
            .get()
            .await()
        return snapshot.exists()
    }

    fun observeMembers(groupId: String): Flow<List<Member>> = callbackFlow {
        val ref = database.getReference("members").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = snapshot.children.mapNotNull {
                    it.getValue(Member::class.java)
                }
                trySend(members)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Announcements ──────────────────────────────────────────────────────────

    suspend fun sendAnnouncement(announcement: Announcement): String {
        val ref = database.getReference("announcements")
            .child(announcement.groupId)
            .push()
        val announcementId = ref.key!!
        ref.setValue(announcement.copy(announcementId = announcementId)).await()
        return announcementId
    }

    fun observeAnnouncements(groupId: String): Flow<List<Announcement>> = callbackFlow {
        val ref = database.getReference("announcements").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val announcements = snapshot.children.mapNotNull {
                    it.getValue(Announcement::class.java)
                }.sortedByDescending { it.createdAt }
                trySend(announcements)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── FCM Tokens ─────────────────────────────────────────────────────────────

    suspend fun saveFcmToken(uid: String, token: String) {
        database.getReference("fcm_tokens")
            .child(uid)
            .setValue(mapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )).await()
    }

    suspend fun getFcmToken(uid: String): String? {
        val snapshot = database.getReference("fcm_tokens")
            .child(uid)
            .get()
            .await()
        return snapshot.child("token").getValue(String::class.java)
    }

    suspend fun getAllGroupMemberTokens(groupId: String): List<String> {
        val membersSnapshot = database.getReference("members")
            .child(groupId)
            .get()
            .await()

        val tokens = mutableListOf<String>()
        for (memberSnap in membersSnapshot.children) {
            val uid = memberSnap.key ?: continue
            val token = getFcmToken(uid)
            if (token != null) tokens.add(token)
        }
        return tokens
    }
}