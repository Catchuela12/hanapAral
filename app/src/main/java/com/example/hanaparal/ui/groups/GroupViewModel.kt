package com.example.hanaparal.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import com.example.hanaparal.data.repository.GroupRepository
import com.example.hanaparal.data.repository.MessagingRepository
import com.example.hanaparal.data.repository.RemoteConfigRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val messagingRepository: MessagingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class GroupUiState {
        object Idle : GroupUiState()
        object Loading : GroupUiState()
        object Success : GroupUiState()
        data class Error(val message: String) : GroupUiState()
    }

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Idle)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _isMember = MutableStateFlow(false)
    val isMember: StateFlow<Boolean> = _isMember.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    // Job tracking for cancellation
    private var groupsJob: Job? = null
    private var groupDetailJob: Job? = null
    private var membersJob: Job? = null
    private var announcementsJob: Job? = null

    val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    // ── Remote Config ──────────────────────────────────────────────────────────

    fun isGroupCreationEnabled(): Boolean =
        remoteConfigRepository.isGroupCreationEnabled()

    fun getMaxMembers(): Int =
        remoteConfigRepository.getMaxMembers()

    fun getAnnouncementHeader(): String =
        remoteConfigRepository.getAnnouncementHeader()

    // ── Observers ─────────────────────────────────────────────────────────────

    fun observeAllGroups() {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            groupRepository.observeAllGroups().collect { groupList ->
                _groups.value = groupList
            }
        }
    }

    fun observeGroup(groupId: String) {
        groupDetailJob?.cancel()
        groupDetailJob = viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                _selectedGroup.value = group
            }
        }
    }

    fun observeMembers(groupId: String) {
        membersJob?.cancel()
        membersJob = viewModelScope.launch {
            groupRepository.observeMembers(groupId).collect { memberList ->
                _members.value = memberList
            }
        }
    }

    fun observeAnnouncements(groupId: String) {
        announcementsJob?.cancel()
        announcementsJob = viewModelScope.launch {
            groupRepository.observeAnnouncements(groupId).collect { announcementList ->
                _announcements.value = announcementList
            }
        }
    }

    // Cancel all active listeners — call on sign out
    fun clearObservers() {
        groupsJob?.cancel()
        groupDetailJob?.cancel()
        membersJob?.cancel()
        announcementsJob?.cancel()
        _groups.value = emptyList()
        _selectedGroup.value = null
        _members.value = emptyList()
        _announcements.value = emptyList()
        _isMember.value = false
    }

    // ── Group Actions ──────────────────────────────────────────────────────────

    fun createGroup(
        name: String,
        subject: String,
        description: String,
        schedule: String,
        creatorDisplayName: String
    ) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            val maxMembers = remoteConfigRepository.getMaxMembers()
            val group = Group(
                name = name,
                subject = subject,
                description = description,
                schedule = schedule,
                creatorId = currentUid,
                adminId = currentUid,
                memberCount = 1,
                maxMembers = maxMembers,
                createdAt = System.currentTimeMillis()
            )
            groupRepository.createGroup(group)
                .onSuccess { groupId ->
                    val member = Member(
                        uid = currentUid,
                        displayName = creatorDisplayName,
                        joinedAt = System.currentTimeMillis()
                    )
                    groupRepository.joinGroup(groupId, member)

                    // Subscribe creator to group topics
                    messagingRepository.subscribeToGroup(groupId)
                    messagingRepository.subscribeToAnnouncements(groupId)

                    // Send study reminder if schedule is set
                    if (schedule.isNotBlank()) {
                        messagingRepository.sendStudyReminder(
                            groupId = groupId,
                            groupName = name,
                            schedule = schedule
                        )
                    }

                    _uiState.value = GroupUiState.Success
                    _events.emit("Group created successfully!")
                }
                .onFailure { e ->
                    _uiState.value = GroupUiState.Error(e.message ?: "Failed to create group")
                    _events.emit(e.message ?: "Failed to create group")
                }
        }
    }

    fun joinGroup(groupId: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            val alreadyMember = groupRepository.isGroupMember(groupId, currentUid)
            if (alreadyMember) {
                _uiState.value = GroupUiState.Idle
                _events.emit("You are already a member of this group")
                return@launch
            }
            val member = Member(
                uid = currentUid,
                displayName = displayName,
                joinedAt = System.currentTimeMillis()
            )
            groupRepository.joinGroup(groupId, member)
                .onSuccess {
                    _uiState.value = GroupUiState.Success
                    _isMember.value = true

                    // Subscribe to group topics
                    messagingRepository.subscribeToGroup(groupId)
                    messagingRepository.subscribeToAnnouncements(groupId)

                    // Get group name for notification
                    val group = _selectedGroup.value
                    if (group != null) {
                        messagingRepository.sendNewMemberNotification(
                            groupId = groupId,
                            groupName = group.name,
                            memberName = displayName
                        )
                    }

                    _events.emit("Successfully joined the group!")
                }
                .onFailure { e ->
                    _uiState.value = GroupUiState.Error(e.message ?: "Failed to join group")
                    _events.emit(e.message ?: "Failed to join group")
                }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            groupRepository.leaveGroup(groupId, currentUid)
                .onSuccess {
                    _uiState.value = GroupUiState.Success
                    _isMember.value = false

                    // Unsubscribe from group topics
                    messagingRepository.unsubscribeFromGroup(groupId)
                    messagingRepository.unsubscribeFromAnnouncements(groupId)

                    _events.emit("You have left the group")
                }
                .onFailure { e ->
                    _uiState.value = GroupUiState.Error(e.message ?: "Failed to leave group")
                    _events.emit(e.message ?: "Failed to leave group")
                }
        }
    }

    fun checkMembership(groupId: String) {
        viewModelScope.launch {
            _isMember.value = groupRepository.isGroupMember(groupId, currentUid)
        }
    }

    fun sendAnnouncement(groupId: String, message: String, senderName: String) {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            val announcement = Announcement(
                groupId = groupId,
                message = message,
                sentBy = currentUid,
                senderName = senderName,
                createdAt = System.currentTimeMillis()
            )
            groupRepository.sendAnnouncement(announcement)
                .onSuccess {
                    // Send FCM notification to all group members
                    val group = _selectedGroup.value
                    if (group != null) {
                        messagingRepository.sendAnnouncementNotification(
                            groupId = groupId,
                            groupName = group.name,
                            senderName = senderName,
                            message = message
                        )
                    }
                    _uiState.value = GroupUiState.Success
                    _events.emit("Announcement sent!")
                }
                .onFailure { e ->
                    _uiState.value = GroupUiState.Error(
                        e.message ?: "Failed to send announcement"
                    )
                    _events.emit(e.message ?: "Failed to send announcement")
                }
        }
    }

    fun resetState() {
        _uiState.value = GroupUiState.Idle
    }
}