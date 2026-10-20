package dz.madrassati.core.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KioskManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val adminComponent = ComponentName(context, KioskAdminReceiver::class.java)

    private val dpm: DevicePolicyManager? =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

    val isDeviceOwner: Boolean
        get() = dpm?.isDeviceOwnerApp(context.packageName) == true

    fun configureIfDeviceOwner() {
        if (!isDeviceOwner) return
        runCatching {
            dpm?.setLockTaskPackages(adminComponent, arrayOf(context.packageName))

            listOf(
                UserManager.DISALLOW_FACTORY_RESET,
                UserManager.DISALLOW_SAFE_BOOT,
                UserManager.DISALLOW_ADD_USER,
                UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA
            ).forEach { restriction -> dpm?.addUserRestriction(adminComponent, restriction) }

            val homeFilter = IntentFilter().apply {
                addAction(Intent.ACTION_MAIN)
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm?.addPersistentPreferredActivity(
                adminComponent,
                homeFilter,
                ComponentName(context, dz.madrassati.MainActivity::class.java)
            )
        }
    }

    fun enterLockTask(activity: Activity) {
        runCatching {
            val manager = activity.getSystemService(ActivityManager::class.java)
            if (manager?.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.startLockTask()
            }
        }
    }

    fun exitLockTask(activity: Activity) {
        runCatching {
            val manager = activity.getSystemService(ActivityManager::class.java)
            if (manager?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
            }
        }
    }
}
