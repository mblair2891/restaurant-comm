package com.restaurantcomm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurantcomm.data.model.DeviceRole

@Composable
fun HomeScreen(
    role: DeviceRole,
    onSendMessageClick: () -> Unit,
    onCannedMessagesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("RestaurantComm", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Current role: ${role.name}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSendMessageClick, modifier = Modifier.fillMaxWidth()) {
            Text("Send Message")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCannedMessagesClick, modifier = Modifier.fillMaxWidth()) {
            Text("Canned Messages")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
    }
}
