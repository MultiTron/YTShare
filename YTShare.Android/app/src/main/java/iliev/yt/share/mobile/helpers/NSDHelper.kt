package iliev.yt.share.mobile.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import iliev.yt.share.mobile.models.HostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.InetAddress

class NSDHelper(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _hosts = MutableStateFlow<List<HostModel>>(emptyList())
    val hosts: StateFlow<List<HostModel>> = _hosts.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var pendingRestart = false

    fun startDiscovery() {
        if (discoveryListener != null) return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: $service")
                if (service.serviceType == SERVICE_TYPE) {
                    nsdManager.resolveService(service, createResolveListener())
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Service discovery stopped")
                discoveryListener = null
                if (pendingRestart) {
                    pendingRestart = false
                    _hosts.value = emptyList()
                    startDiscovery()
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed with error code $errorCode")
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed with error code $errorCode")
                discoveryListener = null
            }
        }

        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun restartDiscovery() {
        if (discoveryListener != null) {
            pendingRestart = true
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Discovery already stopped", e)
                discoveryListener = null
                pendingRestart = false
                _hosts.value = emptyList()
                startDiscovery()
            }
        } else {
            _hosts.value = emptyList()
            startDiscovery()
        }
    }

    fun stopDiscovery() {
        pendingRestart = false
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Discovery already stopped", e)
            }
            discoveryListener = null
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                Log.d(TAG, "Resolved service: $resolvedService")

                val hostname = resolvedService.attributes["hostname"]
                    ?.decodeToString()
                    ?: resolvedService.serviceName

                val port = resolvedService.port
                val addresses = getHostAddresses(resolvedService)

                val validIpv4 = addresses.filter { it is Inet4Address && isValidIPv4(it.hostAddress) }
                if (validIpv4.isEmpty()) return

                val bestIp = pickBestIp(validIpv4) ?: return
                val host = HostModel(bestIp.hostAddress, hostname, port)

                _hosts.update { list ->
                    val filtered = list.filter { it.hostName != hostname }
                    filtered + host
                }
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed with error code $errorCode")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getHostAddresses(service: NsdServiceInfo): List<InetAddress> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.hostAddresses
        } else {
            listOfNotNull(service.host)
        }
    }

    private fun pickBestIp(candidates: List<InetAddress>): InetAddress? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        val deviceSubnet = getDeviceSubnet24() ?: return candidates.first()

        return candidates.firstOrNull { candidate ->
            getSubnet24(candidate.hostAddress) == deviceSubnet
        } ?: candidates.first()
    }

    private fun getDeviceSubnet24(): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val linkProperties: LinkProperties =
            connectivityManager.getLinkProperties(network) ?: return null

        val ipv4Address = linkProperties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }

        return ipv4Address?.let { getSubnet24(it.hostAddress) }
    }

    private fun getSubnet24(ip: String?): String? {
        if (ip == null) return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        return "${parts[0]}.${parts[1]}.${parts[2]}"
    }

    companion object {
        private const val TAG = "NSD"
        private const val SERVICE_TYPE = "_http._tcp."

        fun isValidIPv4(ip: String?): Boolean {
            if (ip == null) return false
            val pattern = Regex(
                """^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"""
            )
            return pattern.matches(ip)
        }
    }
}
