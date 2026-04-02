package com.restaurantcomm.data

import com.restaurantcomm.data.model.CannedMessage
import com.restaurantcomm.data.model.DeviceRole

class CannedMessageRepository {

    fun getForRole(role: DeviceRole): List<CannedMessage> {
        return allMessages.filter { it.senderRole == role }
    }

    private val allMessages = listOf(
        CannedMessage("bar_need_ice", DeviceRole.BAR, "Need ice", "Need ice", "Supplies"),
        CannedMessage("bar_need_silverware", DeviceRole.BAR, "Need silverware", "Need silverware", "Supplies"),
        CannedMessage("bar_86_item", DeviceRole.BAR, "86 item", "86 item", "Inventory"),
        CannedMessage("bar_rush_table", DeviceRole.BAR, "Rush on table", "Rush on table", "Urgent"),
        CannedMessage("bar_check_status", DeviceRole.BAR, "Check order status", "Check order status", "Coordination"),
        CannedMessage("bar_allergy_note", DeviceRole.BAR, "Customer allergy note", "Customer allergy note", "Safety"),
        CannedMessage("kitchen_order_working", DeviceRole.KITCHEN, "Order working", "Order working", "Status"),
        CannedMessage("kitchen_ready_2_min", DeviceRole.KITCHEN, "Ready in 2 min", "Ready in 2 min", "Status"),
        CannedMessage("kitchen_ready_pickup", DeviceRole.KITCHEN, "Ready for pickup", "Ready for pickup", "Status"),
        CannedMessage("kitchen_out_item", DeviceRole.KITCHEN, "Out of item", "Out of item", "Inventory"),
        CannedMessage("kitchen_need_clarification", DeviceRole.KITCHEN, "Need clarification", "Need clarification", "Question"),
        CannedMessage("kitchen_heard", DeviceRole.KITCHEN, "Heard", "Heard", "Acknowledgement")
    )
}
