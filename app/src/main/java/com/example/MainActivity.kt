package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.DDayViewModel
import com.example.ui.screens.AddEditEventScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EventDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: DDayViewModel = viewModel()
            val currentTheme by viewModel.currentTheme.collectAsState()

            val isDark = when (currentTheme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAddEvent = {
                                navController.navigate("add_edit_event")
                            },
                            onNavigateToEditEvent = { eventId ->
                                navController.navigate("add_edit_event?eventId=$eventId")
                            },
                            onNavigateToDetail = { eventId ->
                                navController.navigate("event_detail/$eventId")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    composable(
                        route = "add_edit_event?eventId={eventId}",
                        arguments = listOf(
                            navArgument("eventId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val eventIdStr = backStackEntry.arguments?.getString("eventId")
                        val eventId = eventIdStr?.toLongOrNull()
                        AddEditEventScreen(
                            viewModel = viewModel,
                            eventId = eventId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = "event_detail/{eventId}",
                        arguments = listOf(
                            navArgument("eventId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                        EventDetailScreen(
                            viewModel = viewModel,
                            eventId = eventId,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToEdit = { editId ->
                                navController.navigate("add_edit_event?eventId=$editId") {
                                    // Remove the detail screen from the backstack so that saving the edit
                                    // returns directly to the dashboard, preventing stale state loops.
                                    popUpTo("dashboard") { saveState = true }
                                }
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
