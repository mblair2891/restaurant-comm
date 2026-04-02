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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.restaurantcomm.data.model.DeviceRole

@Composable
fun SetupScreen(
    onRoleSelected: (DeviceRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose this device role",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onRoleSelected(DeviceRole.BAR) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BAR")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onRoleSelected(DeviceRole.KITCHEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("KITCHEN")
        }
    }
}
