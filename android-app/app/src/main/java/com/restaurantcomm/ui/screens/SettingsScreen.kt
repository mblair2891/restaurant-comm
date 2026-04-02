package com.restaurantcomm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.SectionVisibilitySettings
import com.restaurantcomm.viewmodel.SettingsSaveResult

@Composable
fun SettingsScreen(
    currentRole: DeviceRole?,
    currentVisibilitySettings: SectionVisibilitySettings,
    onSaveClick: (DeviceRole, SectionVisibilitySettings, (SettingsSaveResult) -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedRole by remember(currentRole) { mutableStateOf(currentRole ?: DeviceRole.BAR) }
    var visibilitySettings by remember(currentVisibilitySettings) { mutableStateOf(currentVisibilitySettings) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Role Assignment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                RoleOption("BAR", selectedRole == DeviceRole.BAR) { selectedRole = DeviceRole.BAR }
                RoleOption("KITCHEN", selectedRole == DeviceRole.KITCHEN) { selectedRole = DeviceRole.KITCHEN }
                Text("Role changes are applied only after tapping Save.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Main Screen Visibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                VisibilityToggleRow("Active alert area", visibilitySettings.showActiveAlertArea) {
                    visibilitySettings = visibilitySettings.copy(showActiveAlertArea = it)
                }
                VisibilityToggleRow("Discovered device status area", visibilitySettings.showDiscoveredDeviceStatusArea) {
                    visibilitySettings = visibilitySettings.copy(showDiscoveredDeviceStatusArea = it)
                }
                VisibilityToggleRow("Message input area", visibilitySettings.showMessageInputArea) {
                    visibilitySettings = visibilitySettings.copy(showMessageInputArea = it)
                }
                VisibilityToggleRow("Canned messages area", visibilitySettings.showCannedMessagesArea) {
                    visibilitySettings = visibilitySettings.copy(showCannedMessagesArea = it)
                }
                VisibilityToggleRow("Message/history area", visibilitySettings.showMessageHistoryArea) {
                    visibilitySettings = visibilitySettings.copy(showMessageHistoryArea = it)
                }
            }
        }

        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(it, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSaveClick(selectedRole, visibilitySettings) { result ->
                    if (result.success) {
                        onBackClick()
                    } else {
                        errorMessage = result.errorMessage
                    }
                }
            }, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
            Button(onClick = onBackClick, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun RoleOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun VisibilityToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
