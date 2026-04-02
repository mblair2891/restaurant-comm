package com.restaurantcomm.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.SectionVisibilitySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATASTORE_NAME = "restaurant_comm_preferences"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class RoleDataStore(private val context: Context) {

    private val roleKey = stringPreferencesKey("device_role")
    private val activeAlertAreaKey = booleanPreferencesKey("show_active_alert_area")
    private val discoveredDeviceStatusAreaKey = booleanPreferencesKey("show_discovered_device_status_area")
    private val messageInputAreaKey = booleanPreferencesKey("show_message_input_area")
    private val cannedMessagesAreaKey = booleanPreferencesKey("show_canned_messages_area")
    private val messageHistoryAreaKey = booleanPreferencesKey("show_message_history_area")

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val roleFlow: Flow<DeviceRole?> = preferencesFlow
        .map { prefs -> prefs.toRole() }

    val visibilitySettingsFlow: Flow<SectionVisibilitySettings> = preferencesFlow
        .map { prefs ->
            SectionVisibilitySettings(
                showActiveAlertArea = prefs[activeAlertAreaKey] ?: true,
                showDiscoveredDeviceStatusArea = prefs[discoveredDeviceStatusAreaKey] ?: true,
                showMessageInputArea = prefs[messageInputAreaKey] ?: true,
                showCannedMessagesArea = prefs[cannedMessagesAreaKey] ?: true,
                showMessageHistoryArea = prefs[messageHistoryAreaKey] ?: true
            )
        }

    suspend fun saveRole(role: DeviceRole) {
        context.dataStore.edit { prefs ->
            prefs[roleKey] = role.name
        }
    }

    suspend fun clearRole() {
        context.dataStore.edit { prefs ->
            prefs.remove(roleKey)
        }
    }

    suspend fun saveVisibilitySettings(settings: SectionVisibilitySettings) {
        context.dataStore.edit { prefs ->
            prefs[activeAlertAreaKey] = settings.showActiveAlertArea
            prefs[discoveredDeviceStatusAreaKey] = settings.showDiscoveredDeviceStatusArea
            prefs[messageInputAreaKey] = settings.showMessageInputArea
            prefs[cannedMessagesAreaKey] = settings.showCannedMessagesArea
            prefs[messageHistoryAreaKey] = settings.showMessageHistoryArea
        }
    }

    private fun Preferences.toRole(): DeviceRole? {
        val raw = this[roleKey] ?: return null
        return runCatching { DeviceRole.valueOf(raw) }.getOrNull()
    }
}
