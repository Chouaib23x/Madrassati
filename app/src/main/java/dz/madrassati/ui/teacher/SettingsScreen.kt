package dz.madrassati.ui.teacher

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.BuildConfig
import dz.madrassati.R
import dz.madrassati.domain.repository.AuthRepository
import dz.madrassati.ui.common.AppTopBar
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class SettingsErrorKind { EMPTY, SHORT, MISMATCH, WRONG_CURRENT, PIN_FORMAT }

sealed interface SettingsEvent {
    data object PasswordChanged : SettingsEvent
    data object PinChanged : SettingsEvent
    data class Failed(val kind: SettingsErrorKind) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _hasPin = MutableStateFlow(authRepository.hasPin())
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _contentOfficial = MutableStateFlow(prefs.getBoolean(KEY_CONTENT_OFFICIAL, true))
    val contentOfficial: StateFlow<Boolean> = _contentOfficial.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun changePassword(current: String, new: String, confirm: String) {
        when {
            current.isBlank() || new.isBlank() || confirm.isBlank() ->
                viewModelScope.launch { _events.send(SettingsEvent.Failed(SettingsErrorKind.EMPTY)) }

            new.length < 6 ->
                viewModelScope.launch { _events.send(SettingsEvent.Failed(SettingsErrorKind.SHORT)) }

            new != confirm ->
                viewModelScope.launch { _events.send(SettingsEvent.Failed(SettingsErrorKind.MISMATCH)) }

            else -> viewModelScope.launch {
                if (authRepository.changePassword(current, new)) {
                    _events.send(SettingsEvent.PasswordChanged)
                } else {
                    _events.send(SettingsEvent.Failed(SettingsErrorKind.WRONG_CURRENT))
                }
            }
        }
    }

    fun changePin(pin: String, confirm: String) {
        when {
            pin.length != 4 || pin.any { !it.isDigit() } ->
                viewModelScope.launch { _events.send(SettingsEvent.Failed(SettingsErrorKind.PIN_FORMAT)) }

            pin != confirm ->
                viewModelScope.launch { _events.send(SettingsEvent.Failed(SettingsErrorKind.MISMATCH)) }

            else -> viewModelScope.launch {
                authRepository.setPin(pin)
                _hasPin.value = true
                _events.send(SettingsEvent.PinChanged)
            }
        }
    }

    fun setContentOfficial(value: Boolean) {
        _contentOfficial.value = value
        prefs.edit().putBoolean(KEY_CONTENT_OFFICIAL, value).apply()
    }

    companion object {
        private const val KEY_CONTENT_OFFICIAL = "content_official"
    }
}

@Composable
private fun settingsErrorMessage(kind: SettingsErrorKind?): String? = when (kind) {
    SettingsErrorKind.EMPTY -> stringResource(R.string.error_empty_fields)
    SettingsErrorKind.SHORT -> stringResource(R.string.error_password_short)
    SettingsErrorKind.MISMATCH -> stringResource(R.string.error_password_mismatch)
    SettingsErrorKind.WRONG_CURRENT -> stringResource(R.string.error_wrong_credentials)
    SettingsErrorKind.PIN_FORMAT -> stringResource(R.string.error_pin_length)
    null -> null
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onUnlockDevice: () -> Unit,
    onNetwork: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hasPin by viewModel.hasPin.collectAsStateWithLifecycle()
    val contentOfficial by viewModel.contentOfficial.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showPinDialog by rememberSaveable { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<SettingsErrorKind?>(null) }
    var pinError by remember { mutableStateOf<SettingsErrorKind?>(null) }

    val savedMessage = stringResource(R.string.saved_ok)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.PasswordChanged -> {
                    showPasswordDialog = false
                    passwordError = null
                    snackbarHostState.showSnackbar(savedMessage)
                }

                SettingsEvent.PinChanged -> {
                    showPinDialog = false
                    pinError = null
                    snackbarHostState.showSnackbar(savedMessage)
                }

                is SettingsEvent.Failed -> {
                    if (showPasswordDialog) passwordError = event.kind
                    if (showPinDialog) pinError = event.kind
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(text = stringResource(R.string.section_security))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SettingsRow(
                        icon = { Icon(Icons.Filled.Password, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.change_password),
                        onClick = { showPasswordDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = if (hasPin) stringResource(R.string.change_pin)
                        else stringResource(R.string.pin_not_set),
                        onClick = { showPinDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = { Icon(Icons.Filled.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.unlock_action),
                        onClick = onUnlockDevice
                    )
                }
            }
            item {
                SectionHeader(text = stringResource(R.string.section_network))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SettingsRow(
                        icon = { Icon(Icons.Filled.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = stringResource(R.string.network_title),
                        onClick = onNetwork
                    )
                }
            }
            item {
                SectionHeader(text = stringResource(R.string.section_content))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        RadioRow(
                            selected = contentOfficial,
                            text = stringResource(R.string.content_official),
                            onSelect = { viewModel.setContentOfficial(true) }
                        )
                        RadioRow(
                            selected = !contentOfficial,
                            text = stringResource(R.string.content_modified),
                            onSelect = { viewModel.setContentOfficial(false) }
                        )
                    }
                }
            }
            item {
                SectionHeader(text = stringResource(R.string.section_about))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            error = passwordError,
            onDismiss = {
                showPasswordDialog = false
                passwordError = null
            },
            onSave = { current, new, confirm ->
                viewModel.changePassword(current, new, confirm)
            }
        )
    }

    if (showPinDialog) {
        PinChangeDialog(
            error = pinError,
            onDismiss = {
                showPinDialog = false
                pinError = null
            },
            onSave = { pin, confirm -> viewModel.changePin(pin, confirm) }
        )
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
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RadioRow(selected: Boolean, text: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PasswordDialog(
    error: SettingsErrorKind?,
    onDismiss: () -> Unit,
    onSave: (current: String, new: String, confirm: String) -> Unit
) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text(stringResource(R.string.hint_current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text(stringResource(R.string.hint_new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.hint_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                val message = settingsErrorMessage(error)
                if (message != null) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current, new, confirm) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun PinChangeDialog(
    error: SettingsErrorKind?,
    onDismiss: () -> Unit,
    onSave: (pin: String, confirm: String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.hint_new_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { input -> confirm = input.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.hint_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                val message = settingsErrorMessage(error)
                if (message != null) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin, confirm) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
