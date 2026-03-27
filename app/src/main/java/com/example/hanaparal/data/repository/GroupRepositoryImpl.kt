package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
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
class GroupRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase
) : GroupRepository {

    override suspend fun createGroup(group: Group): Result<String> {
        return try {
            val ref = database.getReference("groups").push()
            val groupId = ref.key!!
            val newGroup = group.copy(groupId = groupId)
            ref.setValue(newGroup).await()

            database.getReference("members")
                .child(groupId)
                .child(group.creatorId)
                .setValue(Member(
                    uid = group.creatorId,
                    displayName = "",
                    joinedAt = System.currentTimeMillis()
                )).await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(groupId: String, member: Member): Result<Unit> {
        return try {
            val groupSnapshot = database.getReference("groups")
                .child(groupId)
                .get()
                .await()
            val group = groupSnapshot.getValue(Group::class.java)
                ?: return Result.failure(Exception("Group not found"))

            if (group.memberCount >= group.maxMembers) {
                return Result.failure(Exception("Group is full"))
            }

            database.getReference("members")
                .child(groupId)
                .child(member.uid)
                .setValue(member)
                .await()

            database.getReference("groups")
                .child(groupId)
                .child("memberCount")
                .setValue(group.memberCount + 1)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String, uid: String): Result<Unit> {
        return try {
            database.getReference("members")
                .child(groupId)
                .child(uid)
                .removeValue()
                .await()

            val groupSnapshot = database.getReference("groups")
                .child(groupId)
                .get()
                .await()
            val group = groupSnapshot.getValue(Group::class.java)
            if (group != null) {
                database.getReference("groups")
                    .child(groupId)
                    .child("memberCount")
                    .setValue((group.memberCount - 1).coerceAtLeast(0))
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllGroups(): Flow<List<Group>> = callbackFlow {
        val ref = database.getReference("groups")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull {
                    it.getValue(Group::class.java)
                }
                trySend(groups)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val ref = database.getReference("groups").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Group::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeMembers(groupId: String): Flow<List<Member>> = callbackFlow {
        val ref = database.getReference("members").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = snapshot.children.mapNotNull {
                    it.getValue(Member::class.java)
                }
                trySend(members)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendAnnouncement(announcement: Announcement): Result<Unit> {
        return try {
            val ref = database.getReference("announcements")
                .child(announcement.groupId)
                .push()
            val announcementId = ref.key!!
            ref.setValue(announcement.copy(announcementId = announcementId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> = callbackFlow {
        val ref = database.getReference("announcements").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val announcements = snapshot.children.mapNotNull {
                    it.getValue(Announcement::class.java)
                }.sortedByDescending { it.createdAt }
                trySend(announcements)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun isGroupMember(groupId: String, uid: String): Boolean {
        return try {
            val snapshot = database.getReference("members")
                .child(groupId)
                .child(uid)
                .get()
                .await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }
}