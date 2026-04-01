package com.example.hanaparal.ui.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hanaparal.R
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.ui.components.TopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remoteMsg by viewModel.remoteMsg.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Notifications", actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onPrimary)
                }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            remoteMsg?.let { msg ->
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null)
                        Spacer(Modifier.width(12.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is NotificationsViewModel.UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is NotificationsViewModel.UiState.Empty -> EmptyView(onJoin = { viewModel.joinDemo() })
                    is NotificationsViewModel.UiState.Error -> ErrorView(state.message, onRetry = { viewModel.refresh() })
                    is NotificationsViewModel.UiState.Success -> SuccessView(state.announcements) {
                        sendTestNotification(context)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyView(onJoin: () -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text("No Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Join a group to receive updates.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onJoin) { Text("Join Demo Group") }
    }
}

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("Error", color = MaterialTheme.colorScheme.error)
        Text(msg, textAlign = TextAlign.Center)
        Button(onRetry) { Text("Retry") }
    }
}

@Composable
private fun SuccessView(list: List<Pair<String, Announcement>>, onTest: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onTest, Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Test Notification") } }
        items(list) { NotificationItem(it.second, it.first) }
    }
}

@Composable
private fun NotificationItem(announcement: Announcement, gid: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Announcement", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Text(SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(announcement.createdAt)), fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(announcement.message)
            Text("From: ${announcement.senderName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun sendTestNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "hanaparal_announcements")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Test")
        .setContentText("Notification is working!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(1, notification)
}
