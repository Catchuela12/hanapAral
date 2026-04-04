package com.example.hanaparal

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hanaparal.data.repository.RemoteConfigRepository
import com.example.hanaparal.ui.auth.LoginScreen
import com.example.hanaparal.ui.groups.CreateGroupScreen
import com.example.hanaparal.ui.groups.GroupDetailScreen
import com.example.hanaparal.ui.groups.GroupListScreen
import com.example.hanaparal.ui.notifications.NotificationsScreen
import com.example.hanaparal.ui.profile.ProfileScreen
import com.example.hanaparal.ui.profile.ProfileSetupScreen
import com.example.hanaparal.ui.superuser.SuperuserScreen
import com.example.hanaparal.ui.theme.HanapAralTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            remoteConfigRepository.fetchAndActivate()
        }

        setContent {
            HanapAralTheme {
                HanapAralNavigation()
            }
        }
    }
}

// ── Navigation Routes ──────────────────────────────────────────────────────────

object Routes {
    const val LOGIN = "login"
    const val PROFILE_SETUP = "profile_setup"
    const val GROUP_LIST = "group_list"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val CREATE_GROUP = "create_group"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SUPERUSER = "superuser"

    fun groupDetail(groupId: String) = "group_detail/$groupId"
}

// ── Bottom Nav Items ───────────────────────────────────────────────────────────

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Groups", Routes.GROUP_LIST, Icons.Default.Group),
    BottomNavItem("Notifications", Routes.NOTIFICATIONS, Icons.Default.Notifications),
    BottomNavItem("Profile", Routes.PROFILE, Icons.Default.Person),
)

// ── Main App Composable ────────────────────────────────────────────────────────

@Composable
fun HanapAralNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBottomNav = currentRoute in listOf(
        Routes.LOGIN,
        Routes.PROFILE_SETUP,
        Routes.CREATE_GROUP,
        Routes.GROUP_DETAIL,
        Routes.SUPERUSER
    ) || currentRoute?.startsWith("group_detail/") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!hideBottomNav) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Login ──────────────────────────────────────────────────────────
            composable(Routes.LOGIN) {
                LoginScreen(
                    onAuthenticated = { isNewUser ->
                        if (isNewUser) {
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.GROUP_LIST) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // ── Profile Setup ──────────────────────────────────────────────────
            composable(Routes.PROFILE_SETUP) {
                ProfileSetupScreen(
                    onProfileSaved = {
                        navController.navigate(Routes.GROUP_LIST) {
                            popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        }
                    }
                )
            }

            // ── Group List ─────────────────────────────────────────────────────
            composable(Routes.GROUP_LIST) {
                GroupListScreen(
                    onGroupClick = { groupId ->
                        navController.navigate(Routes.groupDetail(groupId))
                    },
                    onCreateGroup = {
                        navController.navigate(Routes.CREATE_GROUP)
                    }
                )
            }

            // ── Group Detail ───────────────────────────────────────────────────
            composable(
                route = Routes.GROUP_DETAIL,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupDetailScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Create Group ───────────────────────────────────────────────────
            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    onGroupCreated = {
                        navController.navigate(Routes.GROUP_LIST) {
                            popUpTo(Routes.CREATE_GROUP) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Notifications ──────────────────────────────────────────────────
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen()
            }

            // ── Profile ────────────────────────────────────────────────────────
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSuperuser = {
                        navController.navigate(Routes.SUPERUSER)
                    }
                )
            }

            // ── Superuser ──────────────────────────────────────────────────────
            composable(Routes.SUPERUSER) {
                SuperuserScreen()
            }
        }
    }
}

// ── Bottom Navigation Bar ──────────────────────────────────────────────────────

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.GROUP_LIST) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
