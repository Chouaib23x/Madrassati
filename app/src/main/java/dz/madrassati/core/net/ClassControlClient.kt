package dz.madrassati.core.net

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.core.session.ActiveStudent
import dz.madrassati.domain.model.ActivityType
import dz.madrassati.domain.model.Difficulty
import dz.madrassati.domain.model.LessonActivity
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class ClassControlClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val activeStudent: ActiveStudent,
    private val discovery: ServerDiscovery
) {
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _media = MutableStateFlow<MediaDto?>(null)
    val media: StateFlow<MediaDto?> = _media.asStateFlow()

    private val _host = MutableStateFlow<String?>(null)
    val host: StateFlow<String?> = _host.asStateFlow()

    private val lessonEvents = Channel<LessonCommandDto>(Channel.BUFFERED)
    val lessonCommands: Flow<LessonCommandDto> = lessonEvents.receiveAsFlow()

    private var lastSeq = -1L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                poll()
                delay(SchoolNet.CONTROL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _locked.value = false
        _media.value = null
    }

    fun reportProgress(lessonId: Long, lessonTitle: String, position: Int, total: Int) {
        val studentName = activeStudent.name.value ?: return
        scope.launch {
            runCatching {
                val gateway = host() ?: return@runCatching
                val conn = URL("http://$gateway:${SchoolNet.PORT}${SchoolNet.PATH_PROGRESS}")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = CONNECT_TIMEOUT_MS
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                try {
                    val payload = json.encodeToString(
                        ProgressRequest.serializer(),
                        ProgressRequest(
                            deviceId = deviceId(),
                            name = studentName,
                            lessonId = lessonId,
                            lessonTitle = lessonTitle,
                            position = position,
                            total = total
                        )
                    )
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    private fun poll() {
        runCatching {
            val gateway = host() ?: return
            _host.value = gateway
            val conn = URL("http://$gateway:${SchoolNet.PORT}${SchoolNet.PATH_CONTROL}")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = CONNECT_TIMEOUT_MS
            try {
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.readBytes().toString(Charsets.UTF_8)
                    val state = json.decodeFromString(ControlStateDto.serializer(), body)
                    _locked.value = state.locked
                    _media.value = state.media
                    val lesson = state.lesson
                    if (lesson != null && lesson.seq != lastSeq) {
                        lastSeq = lesson.seq
                        lessonEvents.trySend(lesson)
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun fetchExercises(lessonId: Long): List<LessonActivity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val gateway = host() ?: return@runCatching emptyList<LessonActivity>()
                val conn = URL(
                    "http://$gateway:${SchoolNet.PORT}${SchoolNet.PATH_EXERCISES}?lessonId=$lessonId"
                ).openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = CONNECT_TIMEOUT_MS
                try {
                    if (conn.responseCode != 200) {
                        return@runCatching emptyList<LessonActivity>()
                    }
                    val body = conn.inputStream.readBytes().toString(Charsets.UTF_8)
                    json.decodeFromString(ExercisesResponse.serializer(), body)
                        .activities
                        .map { it.toDomain() }
                } finally {
                    conn.disconnect()
                }
            }.getOrDefault(emptyList())
        }

    private fun CustomExerciseDto.toDomain() = LessonActivity(
        id = id,
        lessonId = lessonId,
        type = ActivityType.entries.firstOrNull { it.name == type } ?: ActivityType.QCM,
        difficulty = Difficulty.entries.firstOrNull { it.name == difficulty } ?: Difficulty.MEDIUM,
        sortOrder = sortOrder,
        payload = payload,
        custom = true
    )

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

    companion object {
        private const val CONNECT_TIMEOUT_MS = 1_500
    }
}