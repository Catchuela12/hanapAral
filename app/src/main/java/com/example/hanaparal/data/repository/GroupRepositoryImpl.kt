package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import com.example.hanaparal.data.source.DatabaseDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
