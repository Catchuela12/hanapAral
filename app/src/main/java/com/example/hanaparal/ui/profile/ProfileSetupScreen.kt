package com.example.hanaparal.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.hanaparal.R
import com.example.hanaparal.ui.components.LoadingButton
import com.example.hanaparal.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onProfileSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember {
        mutableStateOf(viewModel.currentUser?.displayName ?: "")
    }
    var course by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var courseError by remember { mutableStateOf(false) }
    var yearError by remember { mutableStateOf(false) }

    val isLoading = uiState is ProfileViewModel.ProfileUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is ProfileViewModel.ProfileUiState.Success) {
            onProfileSaved()
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
            TopBar(title = "Setup Profile")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
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
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Welcome to HanapAral!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Please complete your student profile to get started.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
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
                label = { Text("Course / Program *") },
                placeholder = { Text("e.g. BS Computer Science") },
                modifier = Modifier.fillMaxWidth(),
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
                label = { Text("Year Level *") },
                placeholder = { Text("e.g. 2nd Year") },
                modifier = Modifier.fillMaxWidth(),
                isError = yearError,
                supportingText = {
                    if (yearError) Text(
                        "Year level is required",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                singleLine = true
            )

            // Email (read only from Google)
            OutlinedTextField(
                value = viewModel.currentUser?.email ?: "",
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            LoadingButton(
                text = "Save Profile",
                isLoading = isLoading,
                onClick = {
                    nameError = name.isBlank()
                    courseError = course.isBlank()
                    yearError = year.isBlank()
                    if (!nameError && !courseError && !yearError) {
                        viewModel.saveProfile(name, course, year)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}