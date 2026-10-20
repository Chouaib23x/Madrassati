package dz.madrassati.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.R
import dz.madrassati.domain.repository.AuthRepository
import dz.madrassati.ui.common.AppLogo
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class AuthMode { CREATE, LOGIN }

enum class AuthError { EMPTY, SHORT, MISMATCH, WRONG, TAKEN, WRONG_PIN }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _mode = MutableStateFlow<AuthMode?>(null)
    val mode: StateFlow<AuthMode?> = _mode.asStateFlow()

    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error.asStateFlow()

    private val _recoveryError = MutableStateFlow<AuthError?>(null)
    val recoveryError: StateFlow<AuthError?> = _recoveryError.asStateFlow()

    val initialUsername: String = authRepository.rememberedUsername()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    private val _recovered = Channel<Unit>(Channel.BUFFERED)
    val recovered = _recovered.receiveAsFlow()

    init {
        viewModelScope.launch {
            _mode.value = if (authRepository.hasAccount()) AuthMode.LOGIN else AuthMode.CREATE
        }
    }

    fun submit(username: String, password: String, confirm: String, remember: Boolean) {
        val currentMode = _mode.value ?: return
        _error.value = null
        if (username.isBlank() || password.isBlank() ||
            (currentMode == AuthMode.CREATE && confirm.isBlank())
        ) {
            _error.value = AuthError.EMPTY
            return
        }
        viewModelScope.launch {
            when (currentMode) {
                AuthMode.CREATE -> {
                    if (password.length < 6) {
                        _error.value = AuthError.SHORT
                    } else if (password != confirm) {
                        _error.value = AuthError.MISMATCH
                    } else if (authRepository.createAccount(username, password)) {
                        authRepository.setRememberedUsername(if (remember) username else null)
                        _done.send(Unit)
                    } else {
                        _error.value = AuthError.TAKEN
                    }
                }

                AuthMode.LOGIN -> {
                    if (authRepository.login(username, password)) {
                        authRepository.setRememberedUsername(if (remember) username else null)
                        _done.send(Unit)
                    } else {
                        _error.value = AuthError.WRONG
                    }
                }
            }
        }
    }

    fun recover(pin: String, newPassword: String, confirm: String) {
        _recoveryError.value = null
        if (pin.isBlank() || newPassword.isBlank() || confirm.isBlank()) {
            _recoveryError.value = AuthError.EMPTY
            return
        }
        viewModelScope.launch {
            when {
                !authRepository.verifyPin(pin) -> _recoveryError.value = AuthError.WRONG_PIN
                newPassword.length < 6 -> _recoveryError.value = AuthError.SHORT
                newPassword != confirm -> _recoveryError.value = AuthError.MISMATCH
                else -> {
                    authRepository.resetPassword(newPassword)
                    _recovered.send(Unit)
                }
            }
        }
    }

    fun clearRecoveryError() {
        _recoveryError.value = null
    }
}

@Composable
private fun authErrorMessage(error: AuthError?): String? = when (error) {
    AuthError.EMPTY -> stringResource(R.string.error_empty_fields)
    AuthError.SHORT -> stringResource(R.string.error_password_short)
    AuthError.MISMATCH -> stringResource(R.string.error_password_mismatch)
    AuthError.WRONG -> stringResource(R.string.error_wrong_credentials)
    AuthError.TAKEN -> stringResource(R.string.error_username_taken)
    AuthError.WRONG_PIN -> stringResource(R.string.error_wrong_pin)
    null -> null
}

@Composable
fun AuthScreen(
    onLoggedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val recoveryError by viewModel.recoveryError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onLoggedIn() }
    }

    var username by rememberSaveable { mutableStateOf(viewModel.initialUsername) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(viewModel.initialUsername.isNotBlank()) }
    var showRecovery by rememberSaveable { mutableStateOf(false) }
    var recoveryDone by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.recovered.collect {
            showRecovery = false
            password = ""
            confirm = ""
            recoveryDone = true
        }
    }

    val currentMode = mode
    val isCreate = currentMode == AuthMode.CREATE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo(size = 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(
                if (isCreate) R.string.login_first_title else R.string.teacher_login_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        if (isCreate) {
            Text(
                text = stringResource(R.string.login_first_sub),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(28.dp))

        if (currentMode == null) {
            CircularProgressIndicator()
            return
        }

        val errorMessage = authErrorMessage(error)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.hint_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.hint_password)) },
            singleLine = true,
            isError = errorMessage != null,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                    else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isCreate) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(stringResource(R.string.hint_confirm_password)) },
                singleLine = true,
                isError = errorMessage != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (recoveryDone) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.recovery_ok),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!isCreate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text(
                    text = stringResource(R.string.remember_account),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.submit(username, password, confirm, rememberMe) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(
                    if (isCreate) R.string.action_create_account else R.string.action_login
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!isCreate) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                viewModel.clearRecoveryError()
                showRecovery = true
            }) {
                Text(stringResource(R.string.forgot_password))
            }
        }

        Spacer(Modifier.height(16.dp))
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showRecovery) {
        RecoveryDialog(
            error = recoveryError,
            onDismiss = {
                showRecovery = false
                viewModel.clearRecoveryError()
            },
            onConfirm = { pin, newPassword, confirmPassword ->
                viewModel.recover(pin, newPassword, confirmPassword)
            }
        )
    }
}

@Composable
private fun RecoveryDialog(
    error: AuthError?,
    onDismiss: () -> Unit,
    onConfirm: (pin: String, newPassword: String, confirmPassword: String) -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val errorMessage = authErrorMessage(error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recover_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.recover_password_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.hint_recovery_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.hint_new_password)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.hint_confirm_password)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin, newPassword, confirmPassword) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
