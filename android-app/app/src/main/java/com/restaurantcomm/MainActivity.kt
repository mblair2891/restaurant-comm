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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restaurantcomm.data.CannedMessageRepository
import com.restaurantcomm.data.RoleRepository
import com.restaurantcomm.data.local.RoleDataStore
import com.restaurantcomm.discovery.DiscoveryManager
import com.restaurantcomm.messaging.MessagingRepository
import com.restaurantcomm.ui.components.TabletContainer
import com.restaurantcomm.ui.screens.HomeScreen
import com.restaurantcomm.ui.screens.SettingsScreen
import com.restaurantcomm.ui.theme.RestaurantCommTheme
import com.restaurantcomm.util.SmartReplyEngine
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
    val discoveryState by viewModel.discoveryUiState.collectAsState()
    val messagingState by viewModel.messagingUiState.collectAsState()
    val context = LocalContext.current
    val activeAlertId = messagingState.inboundAlertQueue.firstOrNull()?.id
    var isSettingsOpen by remember { mutableStateOf(false) }

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

            is AppUiState.Ready -> {
                if (isSettingsOpen) {
                    SettingsScreen(
                        currentRole = state.role,
                        currentVisibilitySettings = state.visibilitySettings,
                        onSaveClick = viewModel::saveSettings,
                        onBackClick = { isSettingsOpen = false }
                    )
                } else {
                    HomeScreen(
                        role = state.role,
                        discoveryUiState = discoveryState,
                        messagingUiState = messagingState,
                        visibilitySettings = state.visibilitySettings,
                        onDraftChange = viewModel::updateMessageDraft,
                        onCannedMessageSelected = viewModel::applyCannedMessage,
                        onSelectedPeerChange = viewModel::selectPeer,
                        onSendDirectClick = viewModel::sendDirectMessage,
                        onSendBroadcastClick = viewModel::sendBroadcastMessage,
                        onAcknowledgeActiveAlert = viewModel::acknowledgeActiveAlert,
                        onSmartReplyClick = viewModel::sendSmartReply,
                        onSettingsClick = { isSettingsOpen = true }
                    )
                }
            }
        }
    }
}
