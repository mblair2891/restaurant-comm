package com.restaurantcomm.data

import com.restaurantcomm.data.local.RoleDataStore
import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.SectionVisibilitySettings
import kotlinx.coroutines.flow.Flow

class RoleRepository(private val roleDataStore: RoleDataStore) {

    fun observeRole(): Flow<DeviceRole?> = roleDataStore.roleFlow

    fun observeVisibilitySettings(): Flow<SectionVisibilitySettings> = roleDataStore.visibilitySettingsFlow

    suspend fun saveRole(role: DeviceRole) = roleDataStore.saveRole(role)

    suspend fun resetRole() = roleDataStore.clearRole()

    suspend fun saveVisibilitySettings(settings: SectionVisibilitySettings) =
        roleDataStore.saveVisibilitySettings(settings)
}
