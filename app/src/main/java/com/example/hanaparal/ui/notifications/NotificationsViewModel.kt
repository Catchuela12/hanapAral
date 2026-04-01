package com.example.hanaparal.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.Group
import com.example.hanaparal.data.model.Member
import com.example.hanaparal.data.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: GroupRepository,
    private val auth: FirebaseAuth,
    private val remoteConfig: FirebaseRemoteConfig
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        data class Success(val announcements: List<Pair<String, Announcement>>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _remoteMsg = MutableStateFlow<String?>(null)
    val remoteMsg = _remoteMsg.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            fetchRemoteConfig()
            if (ensureAuth()) loadNotifications()
        }
    }

    private suspend fun ensureAuth(): Boolean {
        if (auth.currentUser != null) return true
        return try {
            auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            _uiState.value = UiState.Error("Auth failed: ${e.message}")
            false
        }
    }

    private suspend fun fetchRemoteConfig() {
        try {
            remoteConfig.fetchAndActivate().await()
            if (remoteConfig.getBoolean("show_announcement")) {
                _remoteMsg.value = remoteConfig.getString("announcement_message")
            } else {
                _remoteMsg.value = null
            }
        } catch (e: Exception) { /* Ignore config errors */ }
    }

    private suspend fun loadNotifications() {
        try {
            val uid = auth.currentUser?.uid ?: return
            val groups = repository.observeAllGroups().first()
            
            val myGroups = groups.filter { repository.isGroupMember(it.groupId, uid) }
            if (myGroups.isEmpty()) {
                _uiState.value = UiState.Empty
                return
            }

            val announcements = myGroups.flatMap { group ->
                repository.observeAnnouncements(group.groupId).first().map { group.groupId to it }
            }.sortedByDescending { it.second.createdAt }

            _uiState.value = if (announcements.isEmpty()) UiState.Empty else UiState.Success(announcements)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Load failed")
        }
    }

    fun joinDemo() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val groups = repository.observeAllGroups().first()
                val gid = if (groups.isEmpty()) {
                    repository.createGroup(Group(name = "Demo Group", creatorId = "system")).getOrThrow()
                } else groups[0].groupId

                val member = Member(uid = auth.currentUser?.uid ?: "", displayName = "Test User")
                repository.joinGroup(gid, member).getOrThrow()
                loadNotifications()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Join failed: ${e.message}")
            }
        }
    }
}
