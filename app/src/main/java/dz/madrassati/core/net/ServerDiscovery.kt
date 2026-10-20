package dz.madrassati.core.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class ServerDiscovery @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _serverIp = MutableStateFlow<String?>(null)
    val serverIp: StateFlow<String?> = _serverIp.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            runCatching {
                val socket = DatagramSocket(SchoolNet.PORT_DISCOVERY)
                socket.soTimeout = RECEIVE_TIMEOUT_MS
                runCatching { socket.broadcast = true }
                val buffer = ByteArray(256)
                var misses = 0
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val payload = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        if (payload == SchoolNet.DISCOVERY_MSG) {
                            val host = packet.address?.hostAddress
                            if (host != null && host != LISTEN_SELF) {
                                _serverIp.value = host
                                misses = 0
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        misses++
                        if (misses >= MISSES_TO_CLEAR) {
                            _serverIp.value = null
                            misses = 0
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _serverIp.value = null
    }

    companion object {
        private const val RECEIVE_TIMEOUT_MS = 1_500
        private const val MISSES_TO_CLEAR = 12
        private const val LISTEN_SELF = "127.0.0.1"
    }
}