package com.linkdeck.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.linkdeck.app.MainActivity
import com.linkdeck.app.R
import com.linkdeck.app.devices.TrustedDeviceStore
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.app.settings.AppMode
import com.linkdeck.app.settings.SettingsStore
import com.linkdeck.core.network.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Persistent foreground service — the notification-shade-first entry point.
 * CONTROLLER shade: Connect / Mirror / Files / Disconnect.
 * HOST shade: Pause sharing / Disconnect.
 */
@AndroidEntryPoint
class LinkDeckService : Service() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var pairingManager: PairingManager
    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var trustedStore: TrustedDeviceStore

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val clipboardMonitor by lazy { com.linkdeck.app.host.HostClipboardMonitor(this) }

    companion object {
        const val CHANNEL_ID = "linkdeck_service"
        const val NOTIFICATION_ID = 42
        const val ACTION_START = "com.linkdeck.app.action.START"
        const val ACTION_STOP = "com.linkdeck.app.action.STOP"
        const val ACTION_DISCONNECT = "com.linkdeck.app.action.DISCONNECT"
        const val ACTION_PAUSE = "com.linkdeck.app.action.PAUSE"
        const val ACTION_OPEN = "com.linkdeck.app.action.OPEN"
        const val EXTRA_ROUTE = "route"

        @Volatile var running = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "linkdeck:connection")
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "linkdeck:connection")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        running = true

        when (intent?.action) {
            ACTION_STOP -> { shutdown(); stopSelf(); return START_NOT_STICKY }
            ACTION_DISCONNECT -> {
                connectionManager.disconnect()
                if (settingsStore.appMode.value == AppMode.HOST) shutdownHost()
            }
            ACTION_PAUSE -> Unit // Phase 4: pause mirroring
            ACTION_OPEN -> {
                val route = intent.getStringExtra(EXTRA_ROUTE) ?: return START_STICKY
                val activity = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_ROUTE, route)
                runCatching { startActivity(activity) }
            }
            else -> startForMode()
        }
        return START_STICKY
    }

    private fun startForMode() {
        when (settingsStore.appMode.value) {
            AppMode.HOST -> {
                pairingManager.hostStart()
                com.linkdeck.app.host.CallMonitor.start(this)
                clipboardMonitor.start()
                acquireLocks()
            }
            AppMode.CONTROLLER -> {
                val last = trustedStore.devices.value.firstOrNull() ?: return
                connectionManager.connect(last)
                acquireLocks()
            }
            null -> Unit
        }
    }

    private fun acquireLocks() {
        runCatching { wakeLock?.takeIf { !it.isHeld }?.acquire(60 * 60 * 1000L) }
        runCatching { wifiLock?.takeIf { !it.isHeld }?.acquire() }
    }

    private fun releaseLocks() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
    }

    private fun shutdownHost() = pairingManager.hostStop()

    private fun shutdown() {
        connectionManager.disconnect()
        shutdownHost()
        releaseLocks()
        running = false
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Re-post to survive launcher swipe-away on aggressive OEMs (Xiaomi/Huawei).
        runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.service_channel_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun action(action: String, route: String? = null): PendingIntent {
        val intent = Intent(this, LinkDeckService::class.java).setAction(action)
        route?.let { intent.putExtra(EXTRA_ROUTE, it) }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val isHost = settingsStore.appMode.value == AppMode.HOST
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_linkdeck)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (isHost) getString(R.string.notif_host_text)
                else getString(R.string.notif_controller_text)
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .apply {
                if (isHost) {
                    addAction(0, getString(R.string.pause), action(ACTION_PAUSE))
                    addAction(0, getString(R.string.disconnect), action(ACTION_DISCONNECT))
                } else {
                    addAction(0, getString(R.string.connect), action(ACTION_START))
                    addAction(0, getString(R.string.feature_mirror), action(ACTION_OPEN, "mirror"))
                    addAction(0, getString(R.string.feature_files), action(ACTION_OPEN, "files"))
                    addAction(0, getString(R.string.disconnect), action(ACTION_DISCONNECT))
                }
                addAction(0, getString(R.string.exit), action(ACTION_STOP))
            }
            .build()
    }
}
