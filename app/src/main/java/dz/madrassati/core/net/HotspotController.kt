package dz.madrassati.core.net

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HotspotState {
    data object Off : HotspotState
    data object Starting : HotspotState
    data class On(val ssid: String, val passphrase: String, val ip: String?) : HotspotState
    data class Failed(val reason: HotspotError) : HotspotState
}

enum class HotspotError { NO_PERMISSION, GENERIC }

@Singleton
class HotspotController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var callback: WifiManager.LocalOnlyHotspotCallback? = null
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private var fixedApEnabled = false

    private val _state = MutableStateFlow<HotspotState>(HotspotState.Off)
    val state: StateFlow<HotspotState> = _state.asStateFlow()

    fun start() {
        val current = _state.value
        if (current is HotspotState.Starting || current is HotspotState.On) return
        _state.value = HotspotState.Starting

        if (tryStartFixedAp()) {
            fixedApEnabled = true
            pollFixedAp()
            return
        }
        startLocalOnlyHotspot()
    }

    fun stop() {
        fixedApEnabled = false
        callback = null
        val res = reservation
        reservation = null
        if (res != null) {
            runCatching { res.close() }
        }
        disableFixedAp()
        _state.value = HotspotState.Off
    }

    private fun tryStartFixedAp(): Boolean = runCatching {
        val method = WifiManager::class.java.getMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            Boolean::class.javaPrimitiveType
        )
        val result = method.invoke(wifi, fixedConfiguration(), true) as? Boolean ?: false
        result
    }.getOrDefault(false)

    private fun disableFixedAp() {
        runCatching {
            val method = WifiManager::class.java.getMethod(
                "setWifiApEnabled",
                WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(wifi, fixedConfiguration(), false)
        }
        runCatching { wifi.isWifiEnabled = true }
    }

    private fun isApEnabled(): Boolean = runCatching {
        val state = wifiApStateMethod?.invoke(wifi) as? Int ?: 0
        state == WIFI_AP_STATE_ENABLED
    }.getOrDefault(false)

    private fun pollFixedAp() {
        scope.launch {
            repeat(20) {
                if (!fixedApEnabled) return@launch
                if (isApEnabled()) {
                    val ip = findHotspotIp()
                    _state.value = HotspotState.On(FIXED_SSID, FIXED_PASS, ip)
                    return@launch
                }
                delay(500)
            }
            if (isApEnabled() && fixedApEnabled) {
                _state.value = HotspotState.On(FIXED_SSID, FIXED_PASS, findHotspotIp())
            } else {
                _state.value = HotspotState.Failed(HotspotError.GENERIC)
            }
        }
    }

    private fun startLocalOnlyHotspot() {
        val cb = object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                reservation = res
                val soft = res.softApConfiguration
                val legacy = res.wifiConfiguration
                val ssid = soft?.ssid.orEmpty()
                    .ifEmpty { legacy?.SSID?.trim('"').orEmpty() }
                val pass = soft?.passphrase.orEmpty()
                    .ifEmpty { legacy?.preSharedKey.orEmpty() }
                publishWhenIpFound(ssid, pass)
            }

            override fun onStopped() {
                reservation = null
                if (callback === this) callback = null
                _state.value = HotspotState.Off
            }

            override fun onFailed(reason: Int) {
                reservation = null
                if (callback === this) callback = null
                _state.value = HotspotState.Failed(HotspotError.GENERIC)
            }
        }
        callback = cb
        try {
            wifi.startLocalOnlyHotspot(cb, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            callback = null
            _state.value = HotspotState.Failed(HotspotError.NO_PERMISSION)
        } catch (e: IllegalStateException) {
            callback = null
            _state.value = HotspotState.Failed(HotspotError.GENERIC)
        }
    }

    private fun fixedConfiguration(): WifiConfiguration {
        val config = WifiConfiguration()
        config.SSID = "\"$FIXED_SSID\""
        config.preSharedKey = "\"$FIXED_PASS\""
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA2_PSK)
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
        config.allowedAuthAlgorithms.clear()
        return config
    }

    private fun publishWhenIpFound(ssid: String, pass: String) {
        scope.launch {
            repeat(20) {
                val ip = findHotspotIp()
                if (ip != null) {
                    _state.value = HotspotState.On(ssid, pass, ip)
                    return@launch
                }
                delay(500)
            }
            _state.value = HotspotState.On(ssid, pass, null)
        }
    }

    private fun findHotspotIp(): String? {
        val candidates = mutableListOf<Pair<Boolean, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                val name = nif.name.lowercase()
                val apLike = name.startsWith("ap") || name.startsWith("swlan") || name.contains("tether")
                for (addr in nif.inetAddresses) {
                    if (addr !is Inet4Address || addr.isLoopbackAddress) continue
                    val host = addr.hostAddress ?: continue
                    if (host.startsWith("192.168.") || host.startsWith("10.")) {
                        candidates += apLike to host
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }
        candidates.sortWith(compareByDescending<Pair<Boolean, String>> { it.first }
            .thenByDescending { it.second.startsWith("192.168.") })
        return candidates.firstOrNull()?.second
    }

    private companion object {
        const val FIXED_SSID = "MADRASATI_SERVER"
        const val FIXED_PASS = "MADRASATI2026"
        const val WIFI_AP_STATE_ENABLED = 13

        val wifiApStateMethod = runCatching {
            WifiManager::class.java.getMethod("getWifiApState")
        }.getOrNull()
    }
}