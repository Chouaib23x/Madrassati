package dz.madrassati.core.net

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object SchoolNet {
    const val PORT = 8767
    const val PORT_DISCOVERY = 8768
    const val DISCOVERY_MSG = "MADRASATI_SERVER"
    const val PATH_PING = "/api/ping"
    const val PATH_HEARTBEAT = "/api/heartbeat"
    const val PATH_DEVICES = "/api/devices"
    const val PATH_STUDENTS = "/api/students"
    const val PATH_CONTROL = "/api/control"
    const val PATH_PROGRESS = "/api/progress"
    const val PATH_STATUS = "/api/status"
    const val PATH_MEDIA_FILE = "/api/media/file"
    const val PATH_EXERCISES = "/api/exercises"
    const val TTL_MS = 30_000L
    const val BEAT_INTERVAL_MS = 4_000L
    const val CONTROL_INTERVAL_MS = 2_000L
    const val DISCOVERY_INTERVAL_MS = 2_000L
    const val SELF_DEVICE_ID = "self"

    fun intToIp(v: Int): String =
        "${v and 0xFF}.${(v shr 8) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 24) and 0xFF}"

    fun broadcastAddress(host: String, prefix: Int): Int? {
        if (prefix < 0 || prefix > 32) return null
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return null
        val ip = parts.fold(0L) { acc, b -> (acc shl 8) or b.toLong() }
        val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        return ((ip and 0xFFFFFFFFL) or ((mask.inv()) and 0xFFFFFFFFL)).toInt()
    }

    fun localIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            var best: String? = null
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr !is Inet4Address || addr.isLoopbackAddress || !addr.isSiteLocalAddress) continue
                    val host = addr.hostAddress ?: continue
                    if (best == null) best = host
                }
            }
            best
        } catch (e: Exception) {
            null
        }
    }

    fun gatewayIp(context: Context): String? {
        val wifi = context.applicationContext
            .getSystemService(WifiManager::class.java) ?: return null
        val dhcp = wifi.dhcpInfo ?: return null
        val gateway = dhcp.gateway
        if (gateway == 0) return null
        return intToIp(gateway)
    }
}

data class DeviceRecord(
    val deviceId: String,
    val name: String,
    val lastSeen: Long
)

class DeviceRegistry(private val ttlMs: Long = SchoolNet.TTL_MS) {

    private val devices = LinkedHashMap<String, DeviceRecord>()

    @Synchronized
    fun heartbeat(deviceId: String, name: String, now: Long) {
        devices[deviceId] = DeviceRecord(deviceId, name, now)
        prune(now)
    }

    @Synchronized
    fun prune(now: Long) {
        devices.values.removeAll { now - it.lastSeen > ttlMs }
    }

    @Synchronized
    fun snapshot(now: Long): List<DeviceRecord> {
        prune(now)
        return devices.values.sortedByDescending { it.lastSeen }
    }

    @Synchronized
    fun clear() {
        devices.clear()
    }
}

data class ControlRecord(
    val deviceId: String,
    val name: String,
    val lastSeen: Long,
    val lessonId: Long?,
    val lessonTitle: String?,
    val position: Int?,
    val total: Int?
)

class ProgressRegistry(private val ttlMs: Long = SchoolNet.TTL_MS) {

    private val records = LinkedHashMap<String, ControlRecord>()

    @Synchronized
    fun heartbeat(deviceId: String, name: String, now: Long) {
        val existing = records[deviceId]
        records[deviceId] = (existing ?: ControlRecord(deviceId, name, now, null, null, null, null))
            .copy(name = name, lastSeen = now)
        prune(now)
    }

    @Synchronized
    fun progress(
        deviceId: String,
        name: String,
        now: Long,
        lessonId: Long,
        lessonTitle: String,
        position: Int,
        total: Int
    ) {
        records[deviceId] = ControlRecord(deviceId, name, now, lessonId, lessonTitle, position, total)
        prune(now)
    }

    @Synchronized
    fun prune(now: Long) {
        records.values.removeAll { now - it.lastSeen > ttlMs }
    }

    @Synchronized
    fun snapshot(now: Long): List<ControlRecord> {
        prune(now)
        return records.values.sortedByDescending { it.lastSeen }
    }

    @Synchronized
    fun clear() {
        records.clear()
    }
}
