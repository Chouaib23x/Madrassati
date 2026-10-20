package dz.madrassati.core.net

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.repository.ClassRepository
import java.net.HttpURLConnection
import java.net.URL
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class StudentSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val classRepository: ClassRepository,
    private val discovery: ServerDiscovery
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _synced = MutableStateFlow(false)
    val synced: StateFlow<Boolean> = _synced.asStateFlow()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val list = fetch()
                if (list != null) {
                    classRepository.importFromServer(list)
                    _students.value = list
                    _synced.value = true
                }
                delay(SchoolNet.BEAT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _synced.value = false
    }

    suspend fun refresh() {
        fetch()?.let { list ->
            classRepository.importFromServer(list)
            _students.value = list
            _synced.value = true
        }
    }

    private fun fetch(): List<Student>? = runCatching {
        val gateway = discovery.serverIp.value ?: SchoolNet.gatewayIp(context) ?: return null
        val conn = URL("http://$gateway:${SchoolNet.PORT}${SchoolNet.PATH_STUDENTS}")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = CONNECT_TIMEOUT_MS
        try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val response = json.decodeFromString(StudentsResponse.serializer(), body)
            response.students.map { dto ->
                Student(dto.id, dto.name, dto.avatar, dto.colorHex, dto.id.toInt())
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    companion object {
        private const val CONNECT_TIMEOUT_MS = 1_500
    }
}