package com.restaurantcomm.data.model

data class SectionVisibilitySettings(
    val showActiveAlertArea: Boolean = true,
    val showDiscoveredDeviceStatusArea: Boolean = true,
    val showMessageInputArea: Boolean = true,
    val showCannedMessagesArea: Boolean = true,
    val showMessageHistoryArea: Boolean = true
)
