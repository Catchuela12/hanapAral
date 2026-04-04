package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import com.example.hanaparal.data.source.DatabaseDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val databaseDataSource: DatabaseDataSource,
    private val messagingRepository: MessagingRepository
) : GroupRepository {

    override fun observeAllGroups(): Flow<List<Group>> =
        databaseDataSource.observeAllGroups()

    override fun observeGroup(groupId: String): Flow<Group?> =
        databaseDataSource.observeGroup(groupId)

    override suspend fun createGroup(group: Group): Result<String> {
        return try {
            val groupId = databaseDataSource.createGroup(group)
            // Creator automatically subscribes to topics
            messagingRepository.subscribeToGroup(groupId)
            messagingRepository.subscribeToAnnouncements(groupId)
    private val firestore: FirebaseFirestore
) : GroupRepository {

    override suspend fun createGroup(group: Group): Result<String> {
        return try {
            val groupRef = firestore.collection("groups").document()
            val groupId = groupRef.id
            val newGroup = group.copy(groupId = groupId)
            
            firestore.runBatch { batch ->
                batch.set(groupRef, newGroup)
                
                val memberRef = groupRef.collection("members").document(group.creatorId)
                batch.set(memberRef, Member(
                    uid = group.creatorId,
                    displayName = "",
                    joinedAt = System.currentTimeMillis()
                ))
            }.await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinGroup(groupId: String, member: Member): Result<Unit> {
        return try {
            databaseDataSource.addMember(groupId, member)
            
            // Subscribe to group topics
            messagingRepository.subscribeToGroup(groupId)
            messagingRepository.subscribeToAnnouncements(groupId)
            
            // Notify existing members
            val group = observeGroup(groupId).firstOrNull()
            if (group != null) {
                messagingRepository.sendNewMemberNotification(
                    groupId = groupId,
                    groupName = group.name,
                    memberName = member.displayName
                )
            }
            
            val groupRef = firestore.collection("groups").document(groupId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(groupRef)
                val group = snapshot.toObject(Group::class.java)
                    ?: throw Exception("Group not found")

                if (group.memberCount >= group.maxMembers) {
                    throw Exception("Group is full")
                }

                val memberRef = groupRef.collection("members").document(member.uid)
                transaction.set(memberRef, member)
                transaction.update(groupRef, "memberCount", FieldValue.increment(1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String, uid: String): Result<Unit> {
        return try {
            databaseDataSource.removeMember(groupId, uid)
            
            // Unsubscribe from topics
            messagingRepository.unsubscribeFromGroup(groupId)
            messagingRepository.unsubscribeFromAnnouncements(groupId)
            
            val groupRef = firestore.collection("groups").document(groupId)
            val memberRef = groupRef.collection("members").document(uid)

            firestore.runBatch { batch ->
                batch.delete(memberRef)
                batch.update(groupRef, "memberCount", FieldValue.increment(-1))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isGroupMember(groupId: String, uid: String): Boolean =
        databaseDataSource.isMember(groupId, uid)

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        databaseDataSource.observeMembers(groupId)

    override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> =
        databaseDataSource.observeAnnouncements(groupId)

    override suspend fun sendAnnouncement(announcement: Announcement): Result<String> {
        return try {
            val announcementId = databaseDataSource.sendAnnouncement(announcement)
            
            // Trigger FCM notification for the announcement
            val group = observeGroup(announcement.groupId).firstOrNull()
            if (group != null) {
                messagingRepository.sendAnnouncementNotification(
                    groupId = announcement.groupId,
                    groupName = group.name,
                    senderName = announcement.senderName,
                    message = announcement.message
                )
            }

            Result.success(announcementId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
    override fun observeAllGroups(): Flow<List<Group>> {
        return firestore.collection("groups")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
            }
    }

    override fun observeGroup(groupId: String): Flow<Group?> {
        return firestore.collection("groups")
            .document(groupId)
            .snapshots()
            .map { it.toObject(Group::class.java) }
    }

    override fun observeMembers(groupId: String): Flow<List<Member>> {
        return firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Member::class.java) }
            }
    }

    override suspend fun sendAnnouncement(announcement: Announcement): Result<Unit> {
        return try {
            val ref = firestore.collection("groups")
                .document(announcement.groupId)
                .collection("announcements")
                .document()
            
            val announcementId = ref.id
            ref.set(announcement.copy(announcementId = announcementId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAnnouncements(groupId: String): Flow<List<Announcement>> {
        return firestore.collection("groups")
            .document(groupId)
            .collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Announcement::class.java) }
            }
    }

    override suspend fun isGroupMember(groupId: String, uid: String): Boolean {
        return try {
            val document = firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .document(uid)
                .get()
                .await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }
}
