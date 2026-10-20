package dz.madrassati.ui.mode

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.R
import dz.madrassati.core.kiosk.KioskManager
import dz.madrassati.ui.common.AppLogo
import dz.madrassati.ui.common.findActivity
import javax.inject.Inject

@HiltViewModel
class ModeViewModel @Inject constructor(
    private val kiosk: KioskManager
) : ViewModel() {

    fun onModeChosen(activity: Activity) {
        kiosk.enterLockTask(activity)
    }
}

@Composable
fun ModeSelectScreen(
    onTeacher: () -> Unit,
    onStudent: () -> Unit,
    viewModel: ModeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    fun choose(onNavigate: () -> Unit) {
        viewModel.onModeChosen(context.findActivity())
        onNavigate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo(size = 112.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.mode_select_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))

        ModeCard(
            title = stringResource(R.string.mode_teacher),
            subtitle = stringResource(R.string.mode_teacher_sub),
            tint = MaterialTheme.colorScheme.primary,
            icon = { Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { choose(onTeacher) }
        )
        Spacer(Modifier.height(20.dp))
        ModeCard(
            title = stringResource(R.string.mode_student),
            subtitle = stringResource(R.string.mode_student_sub),
            tint = MaterialTheme.colorScheme.secondary,
            icon = { Icon(Icons.Filled.Face, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { choose(onStudent) }
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    tint: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
