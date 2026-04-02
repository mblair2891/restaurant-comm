package com.restaurantcomm.data.model

import org.json.JSONObject

data class Message(
    val id: String,
    val fromDeviceId: String,
    val fromRole: DeviceRole,
    val toRole: DeviceRole?,
    val type: MessageType,
    val body: String,
    val timestamp: Long,
    val status: MessageStatus,
    val replyToMessageId: String?
) {
    fun toJsonString(): String {
        val json = JSONObject()
            .put("id", id)
            .put("fromDeviceId", fromDeviceId)
            .put("fromRole", fromRole.name)
            .put("toRole", toRole?.name)
            .put("type", type.name)
            .put("body", body)
            .put("timestamp", timestamp)
            .put("status", status.name)
            .put("replyToMessageId", replyToMessageId)
        return json.toString()
    }

    companion object {
        fun fromJsonString(value: String): Message {
            val json = JSONObject(value)
            val toRole = if (json.isNull("toRole")) null else DeviceRole.valueOf(json.getString("toRole"))
            val replyToMessageId = if (json.isNull("replyToMessageId")) null else json.getString("replyToMessageId")

            return Message(
                id = json.getString("id"),
                fromDeviceId = json.getString("fromDeviceId"),
                fromRole = DeviceRole.valueOf(json.getString("fromRole")),
                toRole = toRole,
                type = MessageType.valueOf(json.getString("type")),
                body = json.getString("body"),
                timestamp = json.getLong("timestamp"),
                status = MessageStatus.valueOf(json.getString("status")),
                replyToMessageId = replyToMessageId
            )
        }
    }
}
