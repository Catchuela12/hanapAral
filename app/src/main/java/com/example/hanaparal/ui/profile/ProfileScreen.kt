package com.example.hanaparal.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.hanaparal.ui.components.LoadingButton
import com.example.hanaparal.ui.components.TopBar
import com.example.hanaparal.ui.groups.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    groupViewModel: GroupViewModel = hiltViewModel(),
    onSignOut: () -> Unit,
    onNavigateToSuperuser: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val superuserEmail by viewModel.superuserEmail.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var courseError by remember { mutableStateOf(false) }
    var yearError by remember { mutableStateOf(false) }

    val isLoading = uiState is ProfileViewModel.ProfileUiState.Loading

    // Reactive superuser check — updates when either profile or config loads
    val isSuperuser = profile != null &&
            superuserEmail.isNotEmpty() &&
            profile!!.email == superuserEmail

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            course = it.course
            year = it.year
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileViewModel.ProfileUiState.Success) {
            isEditing = false
            viewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopBar(title = "My Profile") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Photo
                if (viewModel.currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = viewModel.currentUser?.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile!!.name.firstOrNull()
                                ?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = profile!!.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = profile!!.email,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing,
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(
                            "Full name is required",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    singleLine = true
                )

                // Course Field
                OutlinedTextField(
                    value = course,
                    onValueChange = {
                        course = it
                        courseError = false
                    },
                    label = { Text("Course / Program") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing,
                    isError = courseError,
                    supportingText = {
                        if (courseError) Text(
                            "Course is required",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    singleLine = true
                )

                // Year Field
                OutlinedTextField(
                    value = year,
                    onValueChange = {
                        year = it
                        yearError = false
                    },
                    label = { Text("Year Level") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEditing,
                    isError = yearError,
                    supportingText = {
                        if (yearError) Text(
                            "Year level is required",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    singleLine = true
                )

                // Email (read only)
                OutlinedTextField(
                    value = profile!!.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Edit / Save Button
                if (isEditing) {
                    LoadingButton(
                        text = "Save Changes",
                        isLoading = isLoading,
                        onClick = {
                            nameError = name.isBlank()
                            courseError = course.isBlank()
                            yearError = year.isBlank()
                            if (!nameError && !courseError && !yearError) {
                                viewModel.updateProfile(name, course, year)
                            }
                        }
                    )
                    TextButton(
                        onClick = {
                            isEditing = false
                            profile?.let {
                                name = it.name
                                course = it.course
                                year = it.year
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Edit Profile")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Superuser Panel — only visible to superuser account
                if (isSuperuser) {
                    OutlinedButton(
                        onClick = onNavigateToSuperuser,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Superuser Panel")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sign Out Button
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        groupViewModel.clearObservers()
                        viewModel.signOut(onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}