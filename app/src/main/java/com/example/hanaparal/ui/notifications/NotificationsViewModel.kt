package com.example.hanaparal.ui.notifications

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
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
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

    fun refresh() {
        _uiState.value = NotificationsUiState.Loading
        loadMyGroupAnnouncements()
    }
}