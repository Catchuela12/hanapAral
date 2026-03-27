package com.example.hanaparal.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hanaparal.ui.components.LoadingButton
import com.example.hanaparal.ui.components.TopBar
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupViewModel = hiltViewModel(),
    onGroupCreated: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var subjectError by remember { mutableStateOf(false) }

    val isLoading = uiState is GroupViewModel.GroupUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is GroupViewModel.GroupUiState.Success) {
            onGroupCreated()
            viewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Create Study Group",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fill in the details below to create your study group.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Group Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Group Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = {
                    if (nameError) Text(
                        "Group name is required",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                singleLine = true
            )

            // Subject
            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                    subjectError = false
                },
                label = { Text("Subject / Course *") },
                modifier = Modifier.fillMaxWidth(),
                isError = subjectError,
                supportingText = {
                    if (subjectError) Text(
                        "Subject is required",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Schedule
            OutlinedTextField(
                value = schedule,
                onValueChange = { schedule = it },
                label = { Text("Schedule (e.g. Mon/Wed 3PM)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Max members info
            Text(
                text = "Max members: ${viewModel.getMaxMembers()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Submit Button
            LoadingButton(
                text = "Create Group",
                isLoading = isLoading,
                onClick = {
                    nameError = name.isBlank()
                    subjectError = subject.isBlank()
                    if (!nameError && !subjectError) {
                        val displayName = FirebaseAuth.getInstance()
                            .currentUser?.displayName ?: "Unknown"
                        viewModel.createGroup(
                            name = name.trim(),
                            subject = subject.trim(),
                            description = description.trim(),
                            schedule = schedule.trim(),
                            creatorDisplayName = displayName
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}