package com.example.hanaparal.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanaparal.data.model.User
import com.example.hanaparal.data.repository.ProfileRepository
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
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    sealed class ProfileUiState {
        object Idle : ProfileUiState()
        object Loading : ProfileUiState()
        object Success : ProfileUiState()
        data class Error(val message: String) : ProfileUiState()
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    // Expose superuser email as StateFlow so UI reacts when it changes
    private val _superuserEmail = MutableStateFlow("")
    val superuserEmail: StateFlow<String> = _superuserEmail.asStateFlow()

    val currentUser = auth.currentUser

    private var profileJob: Job? = null

    init {
        loadProfile()
        fetchConfig()
    }

    private fun fetchConfig() {
        viewModelScope.launch {
            remoteConfigRepository.fetchAndActivate()
            // Update after fetch completes
            _superuserEmail.value = remoteConfigRepository.getSuperuserEmail()
            android.util.Log.d(
                "SUPERUSER_DEBUG",
                "Fetched superuser email: ${_superuserEmail.value}"
            )
        }
    }

    fun loadProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            profileRepository.observeProfile(uid).collect { user ->
                _profile.value = user
                android.util.Log.d("SUPERUSER_DEBUG", "Profile email: ${user?.email}")
                android.util.Log.d(
                    "SUPERUSER_DEBUG",
                    "isSuperuser: ${user?.email == _superuserEmail.value}"
                )
            }
        }
    }

    fun getSuperuserEmail(): String =
        remoteConfigRepository.getSuperuserEmail()

    fun saveProfile(name: String, course: String, year: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            val uid = auth.currentUser?.uid
            val email = auth.currentUser?.email
            val photoUrl = auth.currentUser?.photoUrl?.toString()

            if (uid == null) {
                _uiState.value = ProfileUiState.Error("User not logged in")
                return@launch
            }

            val user = User(
                uid = uid,
                name = name.trim(),
                course = course.trim(),
                year = year.trim(),
                email = email ?: "",
                photoUrl = photoUrl ?: "",
                fcmToken = ""
            )

            profileRepository.saveProfile(user)
                .onSuccess {
                    _uiState.value = ProfileUiState.Success
                    _events.emit("Profile saved successfully!")
                }
                .onFailure { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to save profile")
                    _events.emit(e.message ?: "Failed to save profile")
                }
        }
    }

    fun updateProfile(name: String, course: String, year: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            val uid = auth.currentUser?.uid ?: run {
                _uiState.value = ProfileUiState.Error("User not logged in")
                return@launch
            }

            val updates = mapOf(
                "name" to name.trim(),
                "course" to course.trim(),
                "year" to year.trim()
            )

            profileRepository.updateProfile(uid, updates)
                .onSuccess {
                    _uiState.value = ProfileUiState.Success
                    _events.emit("Profile updated successfully!")
                }
                .onFailure { e ->
                    _uiState.value = ProfileUiState.Error(e.message ?: "Failed to update profile")
                    _events.emit(e.message ?: "Failed to update profile")
                }
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch {
            profileJob?.cancel()
            _profile.value = null
            _superuserEmail.value = ""
            auth.signOut()
            onSignOut()
        }
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }
}