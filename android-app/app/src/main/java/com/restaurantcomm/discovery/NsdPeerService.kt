package com.restaurantcomm.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.restaurantcomm.data.model.DeviceRole

internal class NsdPeerService(
    context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onDiscoveryStarted()
        fun onDiscoveryFailed(errorCode: Int)
        fun onPeerFound(peer: DiscoveredDevice)
        fun onPeerLost(serviceName: String?)
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startAdvertising(deviceId: String, role: DeviceRole, port: Int) {
        stopAdvertising()
        val serviceName =
            "${DiscoveryConstants.SERVICE_NAME_PREFIX}-${role.name.lowercase()}-${deviceId.take(8)}"

        val serviceInfo = NsdServiceInfo().apply {
            serviceType = DiscoveryConstants.SERVICE_TYPE
            this.serviceName = serviceName
            setPort(port)
            setAttribute(DiscoveryConstants.TXT_KEY_DEVICE_ID, deviceId)
            setAttribute(DiscoveryConstants.TXT_KEY_ROLE, role.name)
        }

        val listenerImpl = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                listener.onDiscoveryFailed(errorCode)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }

        registrationListener = listenerImpl
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listenerImpl)
    }

    fun startDiscovery(selfDeviceId: String) {
        stopDiscovery()
        val listenerImpl = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                listener.onDiscoveryFailed(errorCode)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                listener.onDiscoveryFailed(errorCode)
            }

            override fun onDiscoveryStarted(serviceType: String) {
                listener.onDiscoveryStarted()
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != DiscoveryConstants.SERVICE_TYPE) {
                    return
                }
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val peer = resolvedInfo.toPeerOrNull(selfDeviceId) ?: return
                        listener.onPeerFound(peer)
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val lostServiceName = serviceInfo.serviceName as? String
                listener.onPeerLost(lostServiceName)
            }
        }

        discoveryListener = listenerImpl
        nsdManager.discoverServices(
            DiscoveryConstants.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            listenerImpl
        )
    }

    fun stop() {
        stopDiscovery()
        stopAdvertising()
    }

    private fun stopAdvertising() {
        registrationListener?.let {
            runCatching { nsdManager.unregisterService(it) }
            registrationListener = null
        }
    }

    private fun stopDiscovery() {
        discoveryListener?.let {
            runCatching { nsdManager.stopServiceDiscovery(it) }
            discoveryListener = null
        }
    }

    private fun NsdServiceInfo.toPeerOrNull(selfDeviceId: String): DiscoveredDevice? {
        val attributes = attributes
        val deviceId = (attributes[DiscoveryConstants.TXT_KEY_DEVICE_ID] as? ByteArray)
            ?.toString(Charsets.UTF_8)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        if (deviceId == selfDeviceId) {
            return null
        }

        val roleRaw = (attributes[DiscoveryConstants.TXT_KEY_ROLE] as? ByteArray)
            ?.toString(Charsets.UTF_8)
            ?: return null
        val role = runCatching { DeviceRole.valueOf(roleRaw) }.getOrNull() ?: return null

        val hostName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hostAddresses.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            host?.hostAddress
        }

        val now = System.currentTimeMillis()

        return DiscoveredDevice(
            deviceId = deviceId,
            role = role,
            host = hostName,
            port = port,
            lastSeen = now,
            isOnline = true
        )
    }
}
