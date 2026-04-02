package com.restaurantcomm.data.model

data class CannedMessage(
    val id: String,
    val senderRole: DeviceRole,
    val label: String,
    val body: String,
    val category: String? = null
)
