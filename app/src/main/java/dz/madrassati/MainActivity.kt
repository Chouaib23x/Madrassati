package dz.madrassati

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import dz.madrassati.core.kiosk.KioskManager
import dz.madrassati.core.theme.MadrassatiTheme
import dz.madrassati.domain.repository.AuthRepository
import dz.madrassati.ui.nav.AppNavHost
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var kiosk: KioskManager

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        runCatching { authRepository.ensureDefaultPin() }
        kiosk.configureIfDeviceOwner()
        if (kiosk.isDeviceOwner) {
            kiosk.enterLockTask(this)
        }
        setContent {
            MadrassatiTheme {
                AppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (kiosk.isDeviceOwner) {
            kiosk.enterLockTask(this)
        }
    }
}
