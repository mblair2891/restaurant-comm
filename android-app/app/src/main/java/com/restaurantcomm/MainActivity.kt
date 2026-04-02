package com.restaurantcomm

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.CannedMessageRepository
import com.restaurantcomm.data.local.RoleDataStore
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.discovery.DiscoveryManager
import com.restaurantcomm.messaging.MessagingRepository
import com.restaurantcomm.ui.components.TabletContainer
import com.restaurantcomm.ui.screens.HomeScreen
import com.restaurantcomm.ui.screens.SettingsScreen
import com.restaurantcomm.ui.screens.SetupScreen
import com.restaurantcomm.ui.theme.RestaurantCommTheme
import com.restaurantcomm.util.SmartReplyEngine
import com.restaurantcomm.util.NavRoutes
import com.restaurantcomm.viewmodel.AppUiState
import com.restaurantcomm.viewmodel.AppViewModel
import com.restaurantcomm.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = RoleRepository(RoleDataStore(applicationContext))
        val discoveryManager = DiscoveryManager(applicationContext)
        val messagingRepository = MessagingRepository()
        val cannedMessageRepository = CannedMessageRepository()
        val smartReplyEngine = SmartReplyEngine()
        val factory = AppViewModelFactory(
            repository = repository,
            discoveryManager = discoveryManager,
            messagingRepository = messagingRepository,
            cannedMessageRepository = cannedMessageRepository,
            smartReplyEngine = smartReplyEngine
        )

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
    val discoveryState by viewModel.discoveryUiState.collectAsState()
    val messagingState by viewModel.messagingUiState.collectAsState()
    val context = LocalContext.current
    val activeAlertId = messagingState.inboundAlertQueue.firstOrNull()?.id

    LaunchedEffect(activeAlertId) {
        if (activeAlertId == null) return@LaunchedEffect
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        uri?.let {
            val ringtone = RingtoneManager.getRingtone(context, it)
            ringtone?.play()
        }
    }

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
                            discoveryUiState = discoveryState,
                            messagingUiState = messagingState,
                            onDraftChange = viewModel::updateMessageDraft,
                            onCannedMessageSelected = viewModel::applyCannedMessage,
                            onSelectedPeerChange = viewModel::selectPeer,
                            onSendDirectClick = viewModel::sendDirectMessage,
                            onSendBroadcastClick = viewModel::sendBroadcastMessage,
                            onAcknowledgeActiveAlert = viewModel::acknowledgeActiveAlert,
                            onSmartReplyClick = viewModel::sendSmartReply,
                            onSettingsClick = { navController.navigate("${NavRoutes.Settings}/${role.name}") }
                        )
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
