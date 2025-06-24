package com.example.ytshare.helpers

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.ytshare.models.HostModel

class NSDHelper(context: Context) {
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_http._tcp."

    public var addresses = arrayListOf<HostModel>()

    fun discoverServices() {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NSD", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("NSD", "Service found: $service")
                if (service.serviceType == serviceType) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            Log.d("NSD", "Resolved service: $resolvedService")
                            val hosts = resolvedService.hostAddresses
                            val port = resolvedService.port

                            val hostname = resolvedService.attributes["hostname"]?.decodeToString() ?: "Unknown"

                            hosts.forEach {
                                host -> if (isValidIPv4WithPort(host.hostAddress)) {
                                    addresses.add(HostModel(host.hostAddress, hostname, port))
                                }
                            }
                        }

                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("NSD", "Resolve failed with error code $errorCode")
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("NSD", "Service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NSD", "Service discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Start discovery failed with error code $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Stop discovery failed with error code $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            fun isValidIPv4WithPort(ip: String?): Boolean {
                val pattern = Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}${'$'}""")
                return pattern.matches(ip.toString())
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }
}