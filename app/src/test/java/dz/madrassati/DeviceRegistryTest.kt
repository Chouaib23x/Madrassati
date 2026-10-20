package dz.madrassati

import dz.madrassati.core.net.DeviceRegistry
import dz.madrassati.core.net.SchoolNet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRegistryTest {

    @Test
    fun heartbeatRegistersDevice() {
        val registry = DeviceRegistry(ttlMs = 30_000)
        registry.heartbeat("id1", "Tablette 1", now = 1_000)
        val snapshot = registry.snapshot(now = 2_000)
        assertEquals(1, snapshot.size)
        assertEquals("Tablette 1", snapshot[0].name)
        assertEquals("id1", snapshot[0].deviceId)
    }

    @Test
    fun staleDevicesArePruned() {
        val registry = DeviceRegistry(ttlMs = 30_000)
        registry.heartbeat("id1", "A", now = 0)
        assertTrue(registry.snapshot(now = 31_000).isEmpty())
    }

    @Test
    fun snapshotSortsByLastSeenDescending() {
        val registry = DeviceRegistry()
        registry.heartbeat("older", "A", now = 1_000)
        registry.heartbeat("newer", "B", now = 5_000)
        val snapshot = registry.snapshot(now = 6_000)
        assertEquals(listOf("newer", "older"), snapshot.map { it.deviceId })
    }

    @Test
    fun duplicateHeartbeatUpdatesRecord() {
        val registry = DeviceRegistry()
        registry.heartbeat("id1", "Old", now = 1_000)
        registry.heartbeat("id1", "New", now = 2_000)
        val snapshot = registry.snapshot(now = 3_000)
        assertEquals(1, snapshot.size)
        assertEquals("New", snapshot[0].name)
        assertEquals(2_000, snapshot[0].lastSeen)
    }

    @Test
    fun intToIpConvertsLittleEndianGateway() {
        val value = 192 or (168 shl 8) or (43 shl 16) or (1 shl 24)
        assertEquals("192.168.43.1", SchoolNet.intToIp(value))
    }
}
