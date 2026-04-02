package com.restaurantcomm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.restaurantcomm.data.model.CannedMessage
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.Message
import com.restaurantcomm.data.model.MessageType
import com.restaurantcomm.data.model.SectionVisibilitySettings
import com.restaurantcomm.discovery.DiscoveryStatus
import com.restaurantcomm.viewmodel.DiscoveryUiState
import com.restaurantcomm.viewmodel.MessagingUiState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    role: DeviceRole?,
    discoveryUiState: DiscoveryUiState,
    messagingUiState: MessagingUiState,
    visibilitySettings: SectionVisibilitySettings,
    onDraftChange: (String) -> Unit,
    onCannedMessageSelected: (CannedMessage) -> Unit,
    onSelectedPeerChange: (String?) -> Unit,
    onSendDirectClick: () -> Unit,
    onSendBroadcastClick: () -> Unit,
    onAcknowledgeActiveAlert: () -> Unit,
    onSmartReplyClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val selectedPeer = discoveryUiState.peers.firstOrNull { it.deviceId == messagingUiState.selectedPeerId }
    val focusManager = LocalFocusManager.current
    val activeAlert = messagingUiState.inboundAlertQueue.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        TopAppBar(
            title = { Text("RestaurantComm Operations") },
            actions = {
                Text(
                    text = "Role: ${role?.name ?: "Not assigned"}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "${discoveryUiState.status.name} • ${discoveryUiState.peers.count()} peers",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Button(onClick = onSettingsClick) { Text("Settings") }
            }
        )

        if (role == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Role not assigned. Open Settings to assign BAR or KITCHEN.")
                    Button(onClick = onSettingsClick) { Text("Settings") }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (visibilitySettings.showActiveAlertArea) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Active Alert", style = MaterialTheme.typography.titleMedium)
                    if (activeAlert == null) {
                        Text("No active alerts")
                    } else {
                        Text("From ${activeAlert.fromRole.name}: ${activeAlert.body}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onAcknowledgeActiveAlert) { Text("Acknowledge") }
                            smartReplyButtons(
                                replies = messagingUiState.smartReplySuggestions,
                                onSmartReplyClick = onSmartReplyClick
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (visibilitySettings.showMessageInputArea) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Message Composer", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = messagingUiState.messageDraft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Message") },
                        maxLines = 3,
                        enabled = role != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onSendDirectClick,
                            modifier = Modifier.weight(1f),
                            enabled = selectedPeer != null && messagingUiState.messageDraft.isNotBlank() && role != null
                        ) {
                            Text("Send Direct")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onSendBroadcastClick,
                            modifier = Modifier.weight(1f),
                            enabled = messagingUiState.messageDraft.isNotBlank() && role != null
                        ) {
                            Text("Broadcast")
                        }
                    }
                    Text(
                        text = "Selected peer: ${selectedPeer?.role?.name ?: "None"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (visibilitySettings.showCannedMessagesArea) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Canned Messages", style = MaterialTheme.typography.titleMedium)
                    val cannedByCategory = remember(messagingUiState.cannedMessages) {
                        messagingUiState.cannedMessages.groupBy { it.category ?: "General" }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cannedByCategory.forEach { (category, cannedMessages) ->
                            Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                cannedMessages.forEach { cannedMessage ->
                                    AssistChip(
                                        onClick = { onCannedMessageSelected(cannedMessage) },
                                        label = { Text(cannedMessage.label) },
                                        enabled = role != null
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (visibilitySettings.showDiscoveredDeviceStatusArea) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Discovered Devices", style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${discoveryUiState.status.name} • Device ID: ${discoveryUiState.deviceId ?: "Pending"}")
                    if (discoveryUiState.peers.isEmpty()) {
                        Text(
                            text = if (discoveryUiState.status == DiscoveryStatus.Active) {
                                "No peers discovered yet on this Wi-Fi network."
                            } else {
                                "Discovery is starting."
                            }
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(130.dp)) {
                            items(discoveryUiState.peers, key = { it.deviceId }) { peer ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    colors = if (peer.deviceId == selectedPeer?.deviceId) {
                                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    } else {
                                        CardDefaults.cardColors()
                                    },
                                    onClick = { onSelectedPeerChange(peer.deviceId) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Role: ${peer.role.name}", fontWeight = FontWeight.SemiBold)
                                        Text("Host: ${peer.host ?: "Unknown"}:${peer.port}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (visibilitySettings.showMessageHistoryArea) {
            MessageHistorySection(
                role = role,
                messages = messagingUiState.messages,
                modifier = Modifier.weight(1f)
            )
        }

        if (activeAlert != null) {
            IncomingAlertDialog(
                message = activeAlert,
                smartReplies = messagingUiState.smartReplySuggestions,
                onAcknowledgeClick = onAcknowledgeActiveAlert,
                onSmartReplyClick = onSmartReplyClick,
                onCloseKeyboardClick = { focusManager.clearFocus(force = true) }
            )
        }
    }
}

@Composable
private fun smartReplyButtons(
    replies: List<String>,
    onSmartReplyClick: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        replies.take(2).forEach { reply ->
            AssistChip(onClick = { onSmartReplyClick(reply) }, label = { Text(reply) })
        }
    }
}

@Composable
private fun MessageHistorySection(
    role: DeviceRole?,
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Direct", "Broadcast", "Alerts / Status")
    val filteredMessages = remember(selectedTab, messages) {
        when (selectedTab) {
            0 -> messages.filter { it.type == MessageType.DIRECT }
            1 -> messages.filter { it.type == MessageType.BROADCAST }
            else -> messages.filter { it.type == MessageType.ACKNOWLEDGEMENT || it.replyToMessageId != null }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Message History", style = MaterialTheme.typography.titleMedium)
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredMessages, key = { it.id }) { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (role != null && message.fromRole == role) {
                                    "Outbound to ${message.toRole?.name ?: "BROADCAST"}"
                                } else {
                                    "Inbound from ${message.fromRole.name}"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = message.body)
                            Text(
                                text = "Type: ${message.type.name} • ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Status: ${message.status.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncomingAlertDialog(
    message: Message,
    smartReplies: List<String>,
    onAcknowledgeClick: () -> Unit,
    onSmartReplyClick: (String) -> Unit,
    onCloseKeyboardClick: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Incoming Message", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("From: ${message.fromRole.name}", fontWeight = FontWeight.SemiBold)
                Text("Message: ${message.body}")
                if (smartReplies.isNotEmpty()) {
                    Text("Smart replies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartReplies.forEach { reply ->
                            AssistChip(
                                onClick = {
                                    onCloseKeyboardClick()
                                    onSmartReplyClick(reply)
                                },
                                label = { Text(reply) }
                            )
                        }
                    }
                }
                Button(onClick = onAcknowledgeClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Acknowledge")
                }
            }
        }
    }
}
