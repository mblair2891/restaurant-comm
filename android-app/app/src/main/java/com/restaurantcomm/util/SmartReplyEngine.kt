package com.restaurantcomm.util

class SmartReplyEngine {

    fun suggestionsFor(messageBody: String): List<String> {
        val normalized = messageBody.trim().lowercase()
        if (normalized.isEmpty()) return fallbackReplies

        return when {
            normalized.contains("need ice") -> listOf("On it", "2 min", "Need quantity?")
            normalized.contains("86 item") -> listOf("Heard", "Update menu", "Which item?")
            normalized.contains("rush on table") -> listOf("Working now", "2 min", "Need table number?")
            normalized.contains("check order status") -> listOf("Plating now", "Ready in 3 min", "Looking now")
            else -> fallbackReplies
        }
    }

    private val fallbackReplies = listOf("Acknowledged", "On the way", "Need more info")
}
