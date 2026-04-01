package com.example.hanaparal.data.repository

import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeAllGroups(): Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group?>
    suspend fun createGroup(group: Group): Result<String>
    suspend fun joinGroup(groupId: String, member: Member): Result<Unit>
    suspend fun leaveGroup(groupId: String, uid: String): Result<Unit>
    suspend fun isGroupMember(groupId: String, uid: String): Boolean
    fun observeMembers(groupId: String): Flow<List<Member>>
    fun observeAnnouncements(groupId: String): Flow<List<Announcement>>
    suspend fun sendAnnouncement(announcement: Announcement): Result<String>
}
