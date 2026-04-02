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

    private val _inboundAlertQueue = MutableStateFlow<List<Message>>(emptyList())
    val inboundAlertQueue: StateFlow<List<Message>> = _inboundAlertQueue.asStateFlow()

    private var selfDeviceId: String? = null
    private var selfRole: DeviceRole? = null
    private var listenerPort: Int = 0

    private val inboundSenderHostByMessageId = mutableMapOf<String, String>()

    fun setSelf(deviceId: String, role: DeviceRole) {
        selfDeviceId = deviceId
        selfRole = role
    }

    fun startListener(port: Int) {
        listenerPort = port
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

    private suspend fun onInboundMessage(inbound: Message, senderHost: String?) {
        if (inbound.type == MessageType.ACKNOWLEDGEMENT) {
            inbound.replyToMessageId?.let { messageId ->
                updateMessageStatus(messageId, MessageStatus.ACKNOWLEDGED)
            }
            return
        }

        val delivered = inbound.copy(status = MessageStatus.DELIVERED)
        appendMessage(delivered)
        senderHost?.let { host -> inboundSenderHostByMessageId[delivered.id] = host }
        _inboundAlertQueue.value = _inboundAlertQueue.value + delivered
    }

    fun acknowledgeMessage(messageId: String, peers: List<DiscoveredDevice>) {
        val acknowledged = updateMessageStatus(messageId, MessageStatus.ACKNOWLEDGED) ?: return
        _inboundAlertQueue.value = _inboundAlertQueue.value.filterNot { it.id == messageId }

        val senderId = selfDeviceId ?: return
        val senderRole = selfRole ?: return
        val host = inboundSenderHostByMessageId[messageId]
        val target = peers.firstOrNull { it.deviceId == acknowledged.fromDeviceId }
            ?: DiscoveredDevice(
                deviceId = acknowledged.fromDeviceId,
                role = acknowledged.fromRole,
                host = host,
                port = listenerPort,
                lastSeen = System.currentTimeMillis(),
                isOnline = host != null
            )

        if (target.host == null) return

        val ackMessage = Message(
            id = UUID.randomUUID().toString(),
            fromDeviceId = senderId,
            fromRole = senderRole,
            toRole = acknowledged.fromRole,
            type = MessageType.ACKNOWLEDGEMENT,
            body = "ACK",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            replyToMessageId = acknowledged.id
        )

        transport.send(target, ackMessage)
    }

    private fun appendMessage(message: Message) {
        _messages.value = (_messages.value + message).sortedBy { it.timestamp }
    }

    private fun replaceMessage(message: Message) {
        _messages.value = _messages.value.map { existing ->
            if (existing.id == message.id) message else existing
        }
    }

    private fun updateMessageStatus(messageId: String, status: MessageStatus): Message? {
        var updatedMessage: Message? = null
        _messages.value = _messages.value.map { existing ->
            if (existing.id == messageId) {
                val updated = existing.copy(status = status)
                updatedMessage = updated
                updated
            } else {
                existing
            }
        }
        return updatedMessage
    }
}
