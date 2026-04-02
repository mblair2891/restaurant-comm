package com.restaurantcomm.discovery

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private const val IDENTITY_DATASTORE_NAME = "restaurant_comm_identity"
private val Context.identityDataStore by preferencesDataStore(name = IDENTITY_DATASTORE_NAME)

class DeviceIdentityStore(private val context: Context) {

    private val deviceIdKey = stringPreferencesKey("device_id")

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.identityDataStore.data.first()[deviceIdKey]
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        context.identityDataStore.edit { preferences ->
            preferences[deviceIdKey] = generated
        }
        return generated
    }
}
