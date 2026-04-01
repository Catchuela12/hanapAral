package com.example.hanaparal.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import com.example.hanaparal.data.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val auth: FirebaseAuth,
    private val fcm: FirebaseMessaging
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class NotificationsUiState {
        object Loading : NotificationsUiState()
        object Empty : NotificationsUiState()
        data class Success(val announcements: List<Pair<String, Announcement>>) : NotificationsUiState()
        data class Error(val message: String) : NotificationsUiState()
    }

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    init {
        logFcmToken()
        ensureAuthenticatedAndLoad()
    }

    private fun logFcmToken() {
        viewModelScope.launch {
            try {
                val token = fcm.token.await()
                Log.d("FCM_TEST", "Current FCM Token: $token")
            } catch (e: Exception) {
                Log.e("FCM_TEST", "Failed to get token: ${e.message}")
            }
        }
    }

    private fun ensureAuthenticatedAndLoad() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (e: Exception) {
                    _uiState.value = NotificationsUiState.Error("Auth failed: ${e.message}")
                    return@launch
                }
            }
            loadMyGroupAnnouncements()
        }
    val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    init {
        loadMyGroupAnnouncements()
    }

    private fun loadMyGroupAnnouncements() {
        viewModelScope.launch {
            try {
                // Get all groups once
                val groups = groupRepository.observeAllGroups().first()

                if (groups.isEmpty()) {
                    _uiState.value = NotificationsUiState.Empty
                    return@launch
                }

                val myGroupIds = mutableListOf<String>()
                for (group in groups) {
                    if (groupRepository.isGroupMember(group.groupId, currentUid)) {
                        myGroupIds.add(group.groupId)
                    }
                }
                // Filter groups the user is a member of
                val myGroupIds = groups
                    .filter { group ->
                        groupRepository.isGroupMember(group.groupId, currentUid)
                    }
                    .map { it.groupId }

                if (myGroupIds.isEmpty()) {
                    _uiState.value = NotificationsUiState.Empty
                    return@launch
                }

                // Collect announcements from all my groups
                val allAnnouncements = mutableListOf<Pair<String, Announcement>>()
                for (groupId in myGroupIds) {
                    val announcements = groupRepository.observeAnnouncements(groupId).first()
                    announcements.forEach { announcement ->
                        allAnnouncements.add(Pair(groupId, announcement))
                    }
                }

                if (allAnnouncements.isEmpty()) {
                    _uiState.value = NotificationsUiState.Empty
                } else {
                    _uiState.value = NotificationsUiState.Success(
                        allAnnouncements.sortedByDescending { it.second.createdAt }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(
                    e.message ?: "Failed to load notifications"
                )
            }
        }
    }

    fun joinDemoGroup() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                // 1. Create a demo group if none exist
                val groups = groupRepository.observeAllGroups().first()
                val targetGroupId = if (groups.isEmpty()) {
                    groupRepository.createGroup(
                        Group(
                            name = "Demo Study Group",
                            subject = "Android Testing",
                            description = "A group for testing notifications",
                            creatorId = "system"
                        )
                    ).getOrThrow()
                } else {
                    groups.first().groupId
                }

                // 2. Join the group
                val member = Member(
                    uid = currentUid,
                    displayName = "Test User",
                    joinedAt = System.currentTimeMillis()
                )
                groupRepository.joinGroup(targetGroupId, member).getOrThrow()

                // 3. Refresh
                loadMyGroupAnnouncements()
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error("Failed to join demo: ${e.message}")
            }
        }
    }

    fun refresh() {
        ensureAuthenticatedAndLoad()
    }
}
    fun refresh() {
        _uiState.value = NotificationsUiState.Loading
        loadMyGroupAnnouncements()
    }
}
