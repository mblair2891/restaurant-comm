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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurantcomm.data.model.CannedMessage
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.Message
import com.restaurantcomm.discovery.DiscoveryStatus
import com.restaurantcomm.viewmodel.DiscoveryUiState
import com.restaurantcomm.viewmodel.MessagingUiState
import androidx.compose.ui.window.Dialog
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    role: DeviceRole,
    discoveryUiState: DiscoveryUiState,
    messagingUiState: MessagingUiState,
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("RestaurantComm", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Current role: ${role.name}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Discovery status: ${discoveryUiState.status.name}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Device ID: ${discoveryUiState.deviceId ?: "Pending…"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Discovered devices", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (discoveryUiState.peers.isEmpty()) {
            Text(
                text = if (discoveryUiState.status == DiscoveryStatus.Active) {
                    "No peers discovered yet on this Wi-Fi network."
                } else {
                    "Discovery is starting."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(modifier = Modifier.height(140.dp)) {
                items(discoveryUiState.peers, key = { it.deviceId }) { peer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = if (peer.deviceId == selectedPeer?.deviceId) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        } else {
                            CardDefaults.cardColors()
                        },
                        onClick = { onSelectedPeerChange(peer.deviceId) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Role: ${peer.role.name}", fontWeight = FontWeight.SemiBold)
                            Text("Host: ${peer.host ?: "Unknown"}:${peer.port}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Canned messages", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
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
                            label = { Text(cannedMessage.label) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = messagingUiState.messageDraft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") },
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onSendDirectClick,
                modifier = Modifier.weight(1f),
                enabled = selectedPeer != null && messagingUiState.messageDraft.isNotBlank()
            ) {
                Text("Send Direct")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSendBroadcastClick,
                modifier = Modifier.weight(1f),
                enabled = messagingUiState.messageDraft.isNotBlank()
            ) {
                Text("Broadcast")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Selected peer: ${selectedPeer?.role?.name ?: "None"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("Messages", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messagingUiState.messages, key = { it.id }) { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (message.fromRole == role) {
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

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
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
                                onClick = { onSmartReplyClick(reply) },
                                label = { Text(reply) }
                            )
                        }
                    }
                }
                Text(
                    text = "Received: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(message.timestamp))}",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Button(onClick = onCloseKeyboardClick) {
                        Text("Close Keyboard")
                    }
                    Button(onClick = onAcknowledgeClick) {
                        Text("Acknowledge")
                    }
                }
            }
        }
    }
}
