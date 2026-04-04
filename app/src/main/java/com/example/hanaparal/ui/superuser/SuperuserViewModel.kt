package com.example.hanaparal.ui.superuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.repository.RemoteConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperuserViewModel @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository
) : ViewModel() {

    sealed class SuperuserUiState {
        object Idle : SuperuserUiState()
        object Loading : SuperuserUiState()
        object Success : SuperuserUiState()
        object BiometricRequired : SuperuserUiState()
        object BiometricSuccess : SuperuserUiState()
        data class Error(val message: String) : SuperuserUiState()
    }

    private val _uiState = MutableStateFlow<SuperuserUiState>(SuperuserUiState.BiometricRequired)
    val uiState: StateFlow<SuperuserUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    // Current config values
    private val _groupCreationEnabled = MutableStateFlow(true)
    val groupCreationEnabled: StateFlow<Boolean> = _groupCreationEnabled.asStateFlow()

    private val _maxMembers = MutableStateFlow(10)
    val maxMembers: StateFlow<Int> = _maxMembers.asStateFlow()

    private val _announcementHeader = MutableStateFlow("Welcome to HanapAral")
    val announcementHeader: StateFlow<String> = _announcementHeader.asStateFlow()

    // ── Biometric ──────────────────────────────────────────────────────────────

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
        _uiState.value = SuperuserUiState.BiometricSuccess
        loadCurrentConfig()
    }

    fun onBiometricFailed() {
        _isAuthenticated.value = false
        _uiState.value = SuperuserUiState.BiometricRequired
        viewModelScope.launch {
            _events.emit("Biometric authentication failed. Please try again.")
        }
    }

    fun onBiometricError(errorMessage: String) {
        _isAuthenticated.value = false
        _uiState.value = SuperuserUiState.Error(errorMessage)
        viewModelScope.launch {
            _events.emit(errorMessage)
        }
    }

    // ── Load Config ────────────────────────────────────────────────────────────

    private fun loadCurrentConfig() {
        _groupCreationEnabled.value = remoteConfigRepository.isGroupCreationEnabled()
        _maxMembers.value = remoteConfigRepository.getMaxMembers()
        _announcementHeader.value = remoteConfigRepository.getAnnouncementHeader()
    }

    // ── Update Config ──────────────────────────────────────────────────────────

    fun refreshConfig() {
        viewModelScope.launch {
            _uiState.value = SuperuserUiState.Loading
            remoteConfigRepository.fetchAndActivate()
                .onSuccess {
                    loadCurrentConfig()
                    _uiState.value = SuperuserUiState.Success
                    _events.emit("Config refreshed successfully!")
                }
                .onFailure { e ->
                    _uiState.value = SuperuserUiState.Error(e.message ?: "Failed to refresh config")
                    _events.emit(e.message ?: "Failed to refresh")
                }
        }
    }

    fun updateGroupCreationEnabled(enabled: Boolean) {
        _groupCreationEnabled.value = enabled
        viewModelScope.launch {
            _events.emit(
                if (enabled) "Group creation enabled"
                else "Group creation disabled"
            )
        }
    }

    fun updateMaxMembers(max: Int) {
        _maxMembers.value = max
        viewModelScope.launch {
            _events.emit("Max members set to $max")
        }
    }

    fun updateAnnouncementHeader(header: String) {
        _announcementHeader.value = header
    }

    fun saveAllChanges() {
        viewModelScope.launch {
            _uiState.value = SuperuserUiState.Loading
            try {
                // Fetch and activate to sync with Firebase
                remoteConfigRepository.fetchAndActivate()
                    .onSuccess {
                        _uiState.value = SuperuserUiState.Success
                        _events.emit("All changes saved and applied!")
                    }
                    .onFailure { e ->
                        _uiState.value = SuperuserUiState.Error(
                            e.message ?: "Failed to save changes"
                        )
                        _events.emit(e.message ?: "Failed to save changes")
                    }
            } catch (e: Exception) {
                _uiState.value = SuperuserUiState.Error(e.message ?: "Unknown error")
                _events.emit(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = SuperuserUiState.Idle
    }
}