package com.example.hanaparal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hanaparal.ui.groups.CreateGroupScreen
import com.example.hanaparal.ui.groups.GroupDetailScreen
import com.example.hanaparal.ui.groups.GroupListScreen
import com.example.hanaparal.ui.theme.HanapAralTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HanapAralTheme {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "group_list"
                ) {
                    composable("group_list") {
                        GroupListScreen(
                            onGroupClick = { groupId ->
                                navController.navigate("group_detail/$groupId")
                            },
                            onCreateGroup = {
                                navController.navigate("create_group")
                            }
                        )
                    }
                    composable("create_group") {
                        CreateGroupScreen(
                            onBackClick = { navController.popBackStack() },
                            onGroupCreated = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "group_detail/{groupId}",
                        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                        GroupDetailScreen(
                            groupId = groupId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

