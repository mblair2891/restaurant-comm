package com.restaurantcomm.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restaurantcomm.data.model.DeviceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATASTORE_NAME = "restaurant_comm_preferences"
private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class RoleDataStore(private val context: Context) {

    private val roleKey = stringPreferencesKey("device_role")

    val roleFlow: Flow<DeviceRole?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toRole() }

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

    private fun Preferences.toRole(): DeviceRole? {
        val raw = this[roleKey] ?: return null
        return runCatching { DeviceRole.valueOf(raw) }.getOrNull()
    }
}
