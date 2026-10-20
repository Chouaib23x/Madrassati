package dz.madrassati

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dz.madrassati.core.net.ClassControlClient
import dz.madrassati.core.net.ClassHeartbeat
import dz.madrassati.core.net.ServerDiscovery
import dz.madrassati.core.net.StudentSync
import dz.madrassati.domain.usecase.SeedContentUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MadrassatiApp : Application() {

    @Inject
    lateinit var seedContent: SeedContentUseCase

    @Inject
    lateinit var heartbeat: ClassHeartbeat

    @Inject
    lateinit var studentSync: StudentSync

    @Inject
    lateinit var controlClient: ClassControlClient

    @Inject
    lateinit var discovery: ServerDiscovery

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { seedContent() }
        }
        discovery.start()
        heartbeat.start()
        studentSync.start()
        controlClient.start()
    }
}
