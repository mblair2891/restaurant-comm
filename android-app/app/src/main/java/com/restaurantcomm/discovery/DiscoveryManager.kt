package com.restaurantcomm.discovery

import android.content.Context
import com.restaurantcomm.data.model.DeviceRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoveryManager(context: Context) : NsdPeerService.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val identityStore = DeviceIdentityStore(context.applicationContext)
    private val nsdPeerService = NsdPeerService(context.applicationContext, this)

    private val discoveredByServiceName = mutableMapOf<String, DiscoveredDevice>()

    private val _status = MutableStateFlow(DiscoveryStatus.Idle)
    val status: StateFlow<DiscoveryStatus> = _status.asStateFlow()

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    private val _peers = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val peers: StateFlow<List<DiscoveredDevice>> = _peers.asStateFlow()

    fun start(role: DeviceRole, port: Int = DEFAULT_PORT) {
        _status.value = DiscoveryStatus.Starting

        scope.launch {
            val selfId = identityStore.getOrCreateDeviceId()
            _deviceId.value = selfId

            nsdPeerService.startAdvertising(deviceId = selfId, role = role, port = port)
            nsdPeerService.startDiscovery(selfDeviceId = selfId)
        }
    }

    fun stop() {
        nsdPeerService.stop()
        discoveredByServiceName.clear()
        _peers.value = emptyList()
        _status.value = DiscoveryStatus.Idle
    }

    override fun onDiscoveryStarted() {
        _status.value = DiscoveryStatus.Active
    }

    override fun onDiscoveryFailed(errorCode: Int) {
        _status.value = DiscoveryStatus.Error
    }

    override fun onPeerFound(peer: DiscoveredDevice) {
        val key = serviceNameKey(peer)
        discoveredByServiceName[key] = peer
        _peers.value = discoveredByServiceName.values.sortedBy { it.role.name }
    }

    override fun onPeerLost(serviceName: String) {
        val normalized = serviceName.lowercase()
        discoveredByServiceName.remove(normalized)
        _peers.value = discoveredByServiceName.values.sortedBy { it.role.name }
    }

    private fun serviceNameKey(peer: DiscoveredDevice): String {
        return "${DiscoveryConstants.SERVICE_NAME_PREFIX}-${peer.role.name.lowercase()}-${peer.deviceId.take(8)}"
    }

    companion object {
        private const val DEFAULT_PORT = 49152
    }
}
