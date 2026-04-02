package com.restaurantcomm.messaging

import com.restaurantcomm.data.model.DeviceRole
import com.restaurantcomm.data.model.Message
import com.restaurantcomm.data.model.MessageStatus
import com.restaurantcomm.data.model.MessageType
import com.restaurantcomm.discovery.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MessagingRepository {

    private val transport = LocalNetworkTransport(onMessageReceived = ::onInboundMessage)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var selfDeviceId: String? = null
    private var selfRole: DeviceRole? = null

    fun setSelf(deviceId: String, role: DeviceRole) {
        selfDeviceId = deviceId
        selfRole = role
    }

    fun startListener(port: Int) {
        transport.start(port)
    }

    fun stopListener() {
        transport.stop()
    }

    fun sendDirect(peer: DiscoveredDevice, body: String): Message? {
        val senderId = selfDeviceId ?: return null
        val senderRole = selfRole ?: return null

        val pending = Message(
            id = UUID.randomUUID().toString(),
            fromDeviceId = senderId,
            fromRole = senderRole,
            toRole = peer.role,
            type = MessageType.DIRECT,
            body = body,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            replyToMessageId = null
        )

        appendMessage(pending)

        val delivered = transport.send(peer, pending)
        if (delivered) {
            val updated = pending.copy(status = MessageStatus.DELIVERED)
            replaceMessage(updated)
            return updated
        }

        return pending
    }

    fun sendBroadcast(peers: List<DiscoveredDevice>, body: String): Message? {
        val senderId = selfDeviceId ?: return null
        val senderRole = selfRole ?: return null

        val pending = Message(
            id = UUID.randomUUID().toString(),
            fromDeviceId = senderId,
            fromRole = senderRole,
            toRole = null,
            type = MessageType.BROADCAST,
            body = body,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            replyToMessageId = null
        )

        appendMessage(pending)

        val reachablePeers = peers.filter { it.deviceId != senderId }
        val delivered = reachablePeers.map { peer -> transport.send(peer, pending) }.any { it }
        if (delivered) {
            val updated = pending.copy(status = MessageStatus.DELIVERED)
            replaceMessage(updated)
            return updated
        }

        return pending
    }

    private suspend fun onInboundMessage(inbound: Message) {
        val delivered = inbound.copy(status = MessageStatus.DELIVERED)
        appendMessage(delivered)
    }

    private fun appendMessage(message: Message) {
        _messages.value = (_messages.value + message).sortedBy { it.timestamp }
    }

    private fun replaceMessage(message: Message) {
        _messages.value = _messages.value.map { existing ->
            if (existing.id == message.id) message else existing
        }
    }
}
