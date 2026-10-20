package dz.madrassati.ui.teacher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.R
import dz.madrassati.core.net.DeviceRecord
import dz.madrassati.core.net.HotspotController
import dz.madrassati.core.net.HotspotError
import dz.madrassati.core.net.HotspotState
import dz.madrassati.core.net.SchoolNet
import dz.madrassati.core.net.SchoolServer
import dz.madrassati.ui.common.AppTopBar
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetMode { HOTSPOT, EXTERNAL }

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val hotspot: HotspotController,
    private val server: SchoolServer
) : ViewModel() {

    val hotspotState = hotspot.state
    val serverRunning = server.running
    val devices = server.devices

    private val _mode = MutableStateFlow(NetMode.HOTSPOT)
    val mode: StateFlow<NetMode> = _mode.asStateFlow()

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp.asStateFlow()

    fun setMode(value: NetMode) {
        if (_mode.value == value) return
        stop()
        _mode.value = value
    }

    fun start() {
        server.start()
        if (_mode.value == NetMode.HOTSPOT) {
            hotspot.start()
        } else {
            refreshIp()
        }
    }

    fun refreshIp() {
        _localIp.value = SchoolNet.localIpv4()
    }

    fun stop() {
        hotspot.stop()
        server.stop()
        _localIp.value = null
    }
}

@Composable
fun NetworkScreen(
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val hotspotState by viewModel.hotspotState.collectAsStateWithLifecycle()
    val serverRunning by viewModel.serverRunning.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val localIp by viewModel.localIp.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var permissionDenied by remember { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(serverRunning, mode) {
        if (serverRunning && mode == NetMode.EXTERNAL) {
            while (true) {
                viewModel.refreshIp()
                delay(2000)
            }
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.start() else permissionDenied = true
    }

    val startNetwork = {
        if (mode == NetMode.EXTERNAL) {
            viewModel.start()
        } else {
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.start() else launcher.launch(permission)
        }
    }

    val deniedMessage = stringResource(R.string.net_error_permission)
    LaunchedEffect(permissionDenied) {
        if (permissionDenied) {
            snackbarHostState.showSnackbar(deniedMessage)
            permissionDenied = false
        }
    }

    val running = if (mode == NetMode.HOTSPOT) {
        hotspotState is HotspotState.On || hotspotState is HotspotState.Starting
    } else {
        serverRunning
    }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.network_title), onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(text = stringResource(R.string.net_mode_title))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = mode == NetMode.HOTSPOT,
                        onClick = { viewModel.setMode(NetMode.HOTSPOT) },
                        label = { Text(stringResource(R.string.net_mode_hotspot)) }
                    )
                    FilterChip(
                        selected = mode == NetMode.EXTERNAL,
                        onClick = { viewModel.setMode(NetMode.EXTERNAL) },
                        label = { Text(stringResource(R.string.net_mode_external)) }
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (mode == NetMode.HOTSPOT) {
                            when (val state = hotspotState) {
                                is HotspotState.Off -> StatusLine(
                                    label = stringResource(R.string.net_hotspot),
                                    value = stringResource(R.string.net_off)
                                )

                                is HotspotState.Starting -> StatusLine(
                                    label = stringResource(R.string.net_hotspot),
                                    value = stringResource(R.string.net_starting)
                                )

                                is HotspotState.On -> {
                                    StatusLine(
                                        label = stringResource(R.string.net_hotspot),
                                        value = stringResource(R.string.net_on)
                                    )
                                    if (state.ssid.isNotEmpty()) {
                                        InfoLine(
                                            label = stringResource(R.string.net_ssid),
                                            value = state.ssid
                                        )
                                    }
                                    if (state.passphrase.isNotEmpty()) {
                                        InfoLine(
                                            label = stringResource(R.string.net_password),
                                            value = state.passphrase
                                        )
                                    }
                                    InfoLine(
                                        label = stringResource(R.string.net_address),
                                        value = state.ip?.let { "http://$it:${SchoolNet.PORT}" }
                                            ?: "—"
                                    )
                                }

                                is HotspotState.Failed -> StatusLine(
                                    label = stringResource(R.string.net_hotspot),
                                    value = stringResource(
                                        when (state.reason) {
                                            HotspotError.NO_PERMISSION -> R.string.net_error_permission
                                            HotspotError.GENERIC -> R.string.net_error_generic
                                        }
                                    )
                                )
                            }
                        } else {
                            StatusLine(
                                label = stringResource(R.string.net_server),
                                value = stringResource(
                                    if (serverRunning) R.string.net_server_running
                                    else R.string.net_server_stopped
                                )
                            )
                            InfoLine(
                                label = stringResource(R.string.net_external_ip),
                                value = localIp?.let { "http://$it:${SchoolNet.PORT}" } ?: "—"
                            )
                        }

                        if (running) {
                            OutlinedButton(
                                onClick = { viewModel.stop() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Filled.WifiOff, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.net_action_stop))
                            }
                        } else {
                            Button(
                                onClick = { startNetwork() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Filled.Wifi, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    stringResource(
                                        if (mode == NetMode.EXTERNAL) R.string.net_start_server
                                        else R.string.net_action_start
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionHeader(text = stringResource(R.string.net_server))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    StatusLine(
                        label = stringResource(R.string.net_server),
                        value = stringResource(
                            if (serverRunning) R.string.net_server_running
                            else R.string.net_server_stopped
                        )
                    )
                }
            }
            item {
                SectionHeader(
                    text = "${stringResource(R.string.net_devices)} (${devices.size})"
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    if (devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.net_no_devices),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        Column {
                            devices.forEachIndexed { index, device ->
                                DeviceRow(device = device, now = now)
                                if (index != devices.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeviceRow(device: DeviceRecord, now: Long) {
    val elapsed = (now - device.lastSeen) / 1000
    val status = when {
        elapsed < 15 -> stringResource(R.string.net_connected_now)
        elapsed < 60 -> stringResource(R.string.net_seen_seconds, elapsed.toInt())
        else -> stringResource(R.string.net_seen_minutes, (elapsed / 60).toInt())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (elapsed < 15) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
