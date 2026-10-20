package dz.madrassati.core.net

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.core.session.ActiveStudent
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class ClassHeartbeat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val activeStudent: ActiveStudent,
    private val discovery: ServerDiscovery
) {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var misses = 0
            while (isActive) {
                if (beat()) {
                    misses = 0
                    _connected.value = true
                } else {
                    misses++
                    if (misses >= MISSES_TO_DISCONNECT) _connected.value = false
                }
                delay(SchoolNet.BEAT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _connected.value = false
    }

    private fun beat(): Boolean = runCatching {
        val gateway = host() ?: return false
        val conn = URL("http://$gateway:${SchoolNet.PORT}${SchoolNet.PATH_HEARTBEAT}")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = CONNECT_TIMEOUT_MS
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        try {
            val payload = json.encodeToString(
                HeartbeatRequest.serializer(),
                HeartbeatRequest(deviceId(), deviceName())
            )
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            conn.responseCode == 200
        } finally {
            conn.disconnect()
        }
    }.getOrElse { false }

    private fun host(): String? =
        discovery.serverIp.value ?: gatewayIp()

    private fun gatewayIp(): String? {
        val dhcp = wifi.dhcpInfo ?: return null
        val gateway = dhcp.gateway
        if (gateway == 0) return null
        return SchoolNet.intToIp(gateway)
    }

    private fun deviceId(): String = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull() ?: "unknown"

    private fun deviceName(): String =
        activeStudent.name.value ?: Build.MODEL ?: "Android"

    companion object {
        private const val CONNECT_TIMEOUT_MS = 1_500
        private const val MISSES_TO_DISCONNECT = 3
    }
}
