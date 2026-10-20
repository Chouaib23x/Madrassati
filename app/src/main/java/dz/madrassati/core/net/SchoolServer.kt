package dz.madrassati.core.net

import android.content.Context
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.BuildConfig
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.repository.ClassRepository
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PingResponse(val app: String, val version: String)

@Serializable
data class HeartbeatRequest(val deviceId: String, val name: String)

@Serializable
data class HeartbeatResponse(val ok: Boolean)

@Serializable
data class DeviceDto(val deviceId: String, val name: String, val lastSeen: Long)

@Serializable
data class DevicesResponse(val devices: List<DeviceDto>, val serverTime: Long)

@Serializable
data class StudentDto(val id: Long, val name: String, val avatar: String, val colorHex: Long)

@Serializable
data class StudentsResponse(val students: List<StudentDto>)

@Serializable
data class LessonCommandDto(val lessonId: Long, val title: String, val seq: Long)

@Serializable
data class MediaDto(val id: Long, val name: String, val mime: String, val seq: Long)

@Serializable
data class CustomExerciseDto(
    val id: Long,
    val lessonId: Long,
    val type: String,
    val difficulty: String,
    val sortOrder: Int,
    val payload: dz.madrassati.domain.model.ActivityPayload
)

@Serializable
data class ExercisesResponse(val activities: List<CustomExerciseDto>)

@Serializable
data class ControlStateDto(
    val locked: Boolean,
    val lesson: LessonCommandDto?,
    val media: MediaDto? = null
)

@Serializable
data class ProgressRequest(
    val deviceId: String,
    val name: String,
    val lessonId: Long,
    val lessonTitle: String,
    val position: Int,
    val total: Int
)

@Serializable
data class ProgressDto(
    val deviceId: String,
    val name: String,
    val lastSeen: Long,
    val lessonId: Long?,
    val lessonTitle: String?,
    val position: Int?,
    val total: Int?
)

@Serializable
data class StatusResponse(val devices: List<ProgressDto>, val control: ControlStateDto)

@Singleton
class SchoolServer @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
    private val classRepository: ClassRepository,
    private val contentRepository: dz.madrassati.domain.repository.ContentRepository
) {
    private val selfId = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull() ?: SchoolNet.SELF_DEVICE_ID

    private val appContext = context.applicationContext
    @Volatile
    private var mediaUri: Uri? = null
    @Volatile
    private var mediaMime: String = ""

    private val registry = DeviceRegistry()
    private val progressRegistry = ProgressRegistry()
    private var engine: ApplicationEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var discoveryJob: Job? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceRecord>>(emptyList())
    val devices: StateFlow<List<DeviceRecord>> = _devices.asStateFlow()

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _lesson = MutableStateFlow<LessonCommandDto?>(null)
    val lesson: StateFlow<LessonCommandDto?> = _lesson.asStateFlow()

    private val _media = MutableStateFlow<MediaDto?>(null)
    val media: StateFlow<MediaDto?> = _media.asStateFlow()

    private val _controlRecords = MutableStateFlow<List<ControlRecord>>(emptyList())
    val controlRecords: StateFlow<List<ControlRecord>> = _controlRecords.asStateFlow()

    private val seqGenerator = java.util.concurrent.atomic.AtomicLong(0L)

    @Synchronized
    fun setLesson(lessonId: Long, title: String) {
        _lesson.value = LessonCommandDto(lessonId, title, seqGenerator.incrementAndGet())
    }

    @Synchronized
    fun clearLesson() {
        _lesson.value = null
    }

    @Synchronized
    fun setMedia(uri: Uri, name: String, mime: String) {
        mediaUri = uri
        mediaMime = mime
        val seq = seqGenerator.incrementAndGet()
        _media.value = MediaDto(seq, name.trim().take(96).ifEmpty { "media" }, mime, seq)
    }

    @Synchronized
    fun clearMedia() {
        mediaUri = null
        mediaMime = ""
        _media.value = null
    }

    @Synchronized
    fun setLocked(value: Boolean) {
        _locked.value = value
    }

    @Synchronized
    fun start() {
        if (engine != null) return
        engine = embeddedServer(CIO, port = SchoolNet.PORT, host = "0.0.0.0") {
            install(ContentNegotiation) { json(json) }
            routing {
                get(SchoolNet.PATH_PING) {
                    call.respond(PingResponse(APP_NAME, BuildConfig.VERSION_NAME))
                }
                post(SchoolNet.PATH_HEARTBEAT) {
                    val body = call.receive<HeartbeatRequest>()
                    val now = System.currentTimeMillis()
                    if (body.deviceId != selfId) {
                        registry.heartbeat(
                            deviceId = body.deviceId.take(64),
                            name = body.name.trim().take(64).ifEmpty { body.deviceId.take(8) },
                            now = now
                        )
                        progressRegistry.heartbeat(
                            deviceId = body.deviceId.take(64),
                            name = body.name.trim().take(64).ifEmpty { body.deviceId.take(8) },
                            now = now
                        )
                        _devices.value = registry.snapshot(now)
                        _controlRecords.value = progressRegistry.snapshot(now)
                    }
                    call.respond(HeartbeatResponse(true))
                }
                get(SchoolNet.PATH_DEVICES) {
                    val now = System.currentTimeMillis()
                    val list = registry.snapshot(now)
                    _devices.value = list
                    call.respondText(
                        text = json.encodeToString(
                            DevicesResponse.serializer(),
                            DevicesResponse(list.map { DeviceDto(it.deviceId, it.name, it.lastSeen) }, now)
                        ),
                        contentType = ContentType.Application.Json
                    )
                }
                get(SchoolNet.PATH_STUDENTS) {
                    val students = classRepository.students().first()
                    call.respondText(
                        text = json.encodeToString(
                            StudentsResponse.serializer(),
                            StudentsResponse(students.map { toDto(it) })
                        ),
                        contentType = ContentType.Application.Json
                    )
                }
                get(SchoolNet.PATH_CONTROL) {
                    call.respond(ControlStateDto(_locked.value, _lesson.value, _media.value))
                }
                get(SchoolNet.PATH_EXERCISES) {
                    val lessonId = call.request.queryParameters["lessonId"]?.toLongOrNull() ?: 0L
                    val custom = contentRepository.activities(lessonId)
                        .first()
                        .filter { it.custom }
                    call.respond(
                        ExercisesResponse(
                            custom.map {
                                CustomExerciseDto(
                                    id = it.id,
                                    lessonId = it.lessonId,
                                    type = it.type.name,
                                    difficulty = it.difficulty.name,
                                    sortOrder = it.sortOrder,
                                    payload = it.payload
                                )
                            }
                        )
                    )
                }
                get(SchoolNet.PATH_MEDIA_FILE) {
                    val uri = mediaUri
                    if (uri == null) {
                        call.respondText("no media", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@get
                    }
                    val type = runCatching { ContentType.parse(mediaMime) }
                        .getOrDefault(ContentType.Application.OctetStream)
                    call.respondOutputStream(contentType = type) {
                        val input = appContext.contentResolver.openInputStream(uri)
                        if (input != null) {
                            input.use { it.copyTo(this) }
                        }
                    }
                }
                post(SchoolNet.PATH_PROGRESS) {
                    val body = call.receive<ProgressRequest>()
                    val now = System.currentTimeMillis()
                    if (body.deviceId != selfId) {
                        progressRegistry.progress(
                            deviceId = body.deviceId.take(64),
                            name = body.name.trim().take(64).ifEmpty { body.deviceId.take(8) },
                            now = now,
                            lessonId = body.lessonId,
                            lessonTitle = body.lessonTitle.trim().take(64),
                            position = body.position,
                            total = body.total
                        )
                        _controlRecords.value = progressRegistry.snapshot(now)
                    }
                    call.respond(HeartbeatResponse(true))
                }
                get(SchoolNet.PATH_STATUS) {
                    val now = System.currentTimeMillis()
                    val list = progressRegistry.snapshot(now)
                    _controlRecords.value = list
                    call.respondText(
                        text = json.encodeToString(
                            StatusResponse.serializer(),
                            StatusResponse(
                                list.map {
                                    ProgressDto(it.deviceId, it.name, it.lastSeen, it.lessonId, it.lessonTitle, it.position, it.total)
                                },
                                ControlStateDto(_locked.value, _lesson.value, _media.value)
                            )
                        ),
                        contentType = ContentType.Application.Json
                    )
                }
            }
        }.start(wait = false)
        _running.value = true
        runDiscovery()
    }

    private fun runDiscovery() {
        if (discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            while (isActive) {
                broadcastDiscovery()
                delay(SchoolNet.DISCOVERY_INTERVAL_MS)
            }
        }
    }

    private fun broadcastDiscovery() {
        val payload = SchoolNet.DISCOVERY_MSG.toByteArray(Charsets.UTF_8)
        for (target in hotspotTargets()) {
            runCatching {
                val (local, broadcast) = target
                val socket = DatagramSocket(0, local)
                socket.broadcast = true
                socket.send(DatagramPacket(payload, payload.size, broadcast, SchoolNet.PORT_DISCOVERY))
                socket.close()
            }
        }
        runCatching {
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.send(
                DatagramPacket(
                    payload,
                    payload.size,
                    InetAddress.getByName("255.255.255.255"),
                    SchoolNet.PORT_DISCOVERY
                )
            )
            socket.close()
        }
    }

    private fun hotspotTargets(): List<Pair<InetAddress, InetAddress>> {
        val result = mutableListOf<Pair<InetAddress, InetAddress>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    if (addr !is Inet4Address || addr.isLoopbackAddress || !addr.isSiteLocalAddress) continue
                    val host = addr.hostAddress ?: continue
                    val prefix = nif.interfaceAddresses
                        .firstOrNull { it.address == addr }
                        ?.networkPrefixLength ?: 24
                    val broadcast = SchoolNet.broadcastAddress(host, prefix.toInt()) ?: continue
                    result += addr to InetAddress.getByName(SchoolNet.intToIp(broadcast))
                }
            }
        } catch (e: Exception) {
            return result
        }
        return result
    }

    @Synchronized
    fun refreshControl() {
        val now = System.currentTimeMillis()
        _controlRecords.value = progressRegistry.snapshot(now)
    }

    @Synchronized
    fun stop() {
        discoveryJob?.cancel()
        discoveryJob = null
        engine?.stop(100, 500)
        engine = null
        registry.clear()
        progressRegistry.clear()
        _devices.value = emptyList()
        _controlRecords.value = emptyList()
        _lesson.value = null
        mediaUri = null
        mediaMime = ""
        _media.value = null
        _running.value = false
    }

    companion object {
        private const val APP_NAME = "MADRASATI"

        private fun toDto(student: Student): StudentDto =
            StudentDto(student.id, student.name, student.avatar, student.colorHex)
    }
}
