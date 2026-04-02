package com.restaurantcomm.discovery

import com.restaurantcomm.data.model.DeviceRole

data class DiscoveredDevice(
    val deviceId: String,
    val role: DeviceRole,
    val host: String?,
    val port: Int,
    val lastSeen: Long,
    val isOnline: Boolean
)
