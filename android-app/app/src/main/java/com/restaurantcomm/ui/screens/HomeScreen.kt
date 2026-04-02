package com.restaurantcomm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.discovery.DiscoveryStatus
import com.restaurantcomm.viewmodel.DiscoveryUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    role: DeviceRole,
    discoveryUiState: DiscoveryUiState,
    onSendMessageClick: () -> Unit,
    onCannedMessagesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(discoveryUiState.peers, key = { it.deviceId }) { peer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Role: ${peer.role.name}", fontWeight = FontWeight.SemiBold)
                            Text("Host: ${peer.host ?: "Unknown"}:${peer.port}")
                            Text(
                                "State: ${if (peer.isOnline) "Discovered" else "Offline"}"
                            )
                            Text(
                                "Last seen: ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(peer.lastSeen))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSendMessageClick, modifier = Modifier.weight(1f)) {
                Text("Send Message")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCannedMessagesClick, modifier = Modifier.weight(1f)) {
                Text("Canned Messages")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
