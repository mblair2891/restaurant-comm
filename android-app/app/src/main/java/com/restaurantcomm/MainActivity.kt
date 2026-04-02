package com.restaurantcomm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.local.RoleDataStore
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.ui.components.TabletContainer
import com.restaurantcomm.ui.screens.CannedMessagesPlaceholderScreen
import com.restaurantcomm.ui.screens.HomeScreen
import com.restaurantcomm.ui.screens.SendMessagePlaceholderScreen
import com.restaurantcomm.ui.screens.SettingsScreen
import com.restaurantcomm.ui.screens.SetupScreen
import com.restaurantcomm.ui.theme.RestaurantCommTheme
import com.restaurantcomm.util.NavRoutes
import com.restaurantcomm.viewmodel.AppUiState
import com.restaurantcomm.viewmodel.AppViewModel
import com.restaurantcomm.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = RoleRepository(RoleDataStore(applicationContext))
        val factory = AppViewModelFactory(repository)

        setContent {
            RestaurantCommTheme {
                val appViewModel: AppViewModel = viewModel(factory = factory)
                RestaurantCommApp(appViewModel)
            }
        }
    }
}

@Composable
private fun RestaurantCommApp(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    TabletContainer {
        when (val state = uiState) {
            AppUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is AppUiState.RoleRequired,
            is AppUiState.Ready -> {
                NavHost(
                    navController = navController,
                    startDestination = if (state is AppUiState.Ready) NavRoutes.Home else NavRoutes.Setup
                ) {
                    composable(NavRoutes.Setup) {
                        SetupScreen(
                            onRoleSelected = { selectedRole ->
                                viewModel.saveRole(selectedRole)
                                navController.navigate(NavRoutes.Home) {
                                    popUpTo(NavRoutes.Setup) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(NavRoutes.Home) {
                        val role = (viewModel.uiState.value as? AppUiState.Ready)?.role
                        if (role == null) {
                            navController.navigate(NavRoutes.Setup) {
                                popUpTo(NavRoutes.Home) { inclusive = true }
                            }
                            return@composable
                        }

                        HomeScreen(
                            role = role,
                            onSendMessageClick = { navController.navigate(NavRoutes.SendMessage) },
                            onCannedMessagesClick = { navController.navigate(NavRoutes.CannedMessages) },
                            onSettingsClick = { navController.navigate("${NavRoutes.Settings}/${role.name}") }
                        )
                    }

                    composable(NavRoutes.SendMessage) {
                        SendMessagePlaceholderScreen()
                    }

                    composable(NavRoutes.CannedMessages) {
                        CannedMessagesPlaceholderScreen()
                    }

                    composable(
                        route = "${NavRoutes.Settings}/{role}",
                        arguments = listOf(navArgument("role") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val roleName = backStackEntry.arguments?.getString("role")
                        val role = runCatching { DeviceRole.valueOf(roleName.orEmpty()) }.getOrNull()
                            ?: (viewModel.uiState.value as? AppUiState.Ready)?.role
                            ?: DeviceRole.BAR

                        SettingsScreen(
                            role = role,
                            onResetRoleClick = {
                                viewModel.resetRole()
                                navController.navigate(NavRoutes.Setup) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
