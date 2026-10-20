package dz.madrassati.ui.student

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dz.madrassati.R
import dz.madrassati.core.session.ActiveStudent
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.repository.ClassRepository
import dz.madrassati.ui.common.AppLogo
import dz.madrassati.ui.common.AppTopBar
import dz.madrassati.ui.common.AvatarView
import dz.madrassati.ui.common.EmptyState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    classRepository: ClassRepository,
    private val activeStudent: ActiveStudent
) : ViewModel() {

    val students: StateFlow<List<Student>> = classRepository.students()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun select(student: Student) {
        activeStudent.activate(student.id, student.name)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentWaitScreen(
    onChooseName: () -> Unit,
    onUnlockGesture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo(
            size = 140.dp,
            modifier = Modifier.combinedClickable(
                enabled = true,
                onClick = {},
                onLongClick = onUnlockGesture
            )
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.student_wait_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.student_wait_sub),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.state_wait),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(200.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onChooseName,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(64.dp)
        ) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.action_choose_name),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun ProfilesScreen(
    onSelect: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val students by viewModel.students.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.pick_profile_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (students.isEmpty()) {
                EmptyState(
                    imageRes = R.drawable.state_empty,
                    text = stringResource(R.string.no_students),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(students) { student ->
                        ProfileCard(student = student) {
                            viewModel.select(student)
                            onSelect(student.id)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileCard(student: Student, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .combinedClickable(enabled = true, onClick = onClick, onLongClick = {}),
        colors = CardDefaults.cardColors(containerColor = Color(student.colorHex)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(avatar = student.avatar, colorHex = 0xFFFFFFFF, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
