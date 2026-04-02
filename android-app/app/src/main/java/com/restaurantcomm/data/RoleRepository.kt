package com.restaurantcomm.data

import com.restaurantcomm.data.local.RoleDataStore
import com.restaurantcomm.data.model.DeviceRole
import kotlinx.coroutines.flow.Flow

class RoleRepository(private val roleDataStore: RoleDataStore) {

    fun observeRole(): Flow<DeviceRole?> = roleDataStore.roleFlow

    suspend fun saveRole(role: DeviceRole) = roleDataStore.saveRole(role)

    suspend fun resetRole() = roleDataStore.clearRole()
}
