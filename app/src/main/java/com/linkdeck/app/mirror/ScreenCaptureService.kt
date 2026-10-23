package com.linkdeck.app.mirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import androidx.core.app.NotificationCompat
import com.linkdeck.app.R
import com.linkdeck.core.network.FramePush
import com.linkdeck.core.network.HostBridge
import com.linkdeck.core.network.ProtoJson
import java.io.ByteArrayOutputStream

/**
 * HOST-side Normal-mode screen capture (fallback for Pro mode):
 * MediaProjection -> JPEG frames over the control channel (~5 fps QVGA).
 * FLAG_SECURE windows render black in the capture — the controller shows the
 * host's explanation message instead of a silent failure.
 */
class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "linkdeck_mirror"
        const val NOTIFICATION_ID = 77
        const val ACTION_START = "com.linkdeck.app.mirror.START"
        const val ACTION_STOP = "com.linkdeck.app.mirror.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"

        @Volatile var running = false
        private var instance: ScreenCaptureService? = null

        fun stop(context: Context) {
            instance?.let { context.startService(Intent(context, ScreenCaptureService::class.java).setAction(ACTION_STOP)) }
        }
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var frameJob: java.util.concurrent.ScheduledFuture<*>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification(getString(R.string.mirror_running)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopCapture(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && data != null) startCapture(resultCode, data)
                else {
                    HostBridge.hostServer?.broadcast("mirror_event",
                        """{"kind":"error","data":"projection_denied"}""")
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = runCatching { pm.getMediaProjection(resultCode, data) }.getOrNull()
        val mp = projection ?: run {
            HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"error","data":"projection_failed"}""")
            return
        }
        val metrics = resources.displayMetrics
        val w = (metrics.widthPixels / 2).coerceAtLeast(320)
        val h = (metrics.heightPixels / 2).coerceAtLeast(480)
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mp.createVirtualDisplay(
            "linkdeck", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler,
        )
        val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        frameJob = scheduler.scheduleWithFixedDelay(::grabFrame, 0, 200, java.util.concurrent.TimeUnit.MILLISECONDS)
        running = true
        instance = this
        HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"started"}""")
    }

    private fun grabFrame() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return
        try {
            val plane = image.planes[0]
            val rowPadding = plane.rowPadding - plane.pixelStride * image.width
            val bmp = Bitmap.createBitmap(
                image.width + rowPadding / plane.pixelStride, image.height, Bitmap.Config.ARGB_8888,
            )
            bmp.copyPixelsFromBuffer(plane.buffer)
            val cropped = Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
            bmp.recycle()
            val out = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, 55, out)
            cropped.recycle()
            val push = FramePush(image.width, image.height, Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
            HostBridge.hostServer?.broadcast("frame", ProtoJson.encodeToString(FramePush.serializer(), push))
        } catch (se: SecurityException) {
            // FLAG_SECURE content: tell the controller instead of failing silently.
            HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"flag_secure"}""")
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    private fun stopCapture() {
        frameJob?.cancel(true)
        frameJob = null
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { projection?.stop() }
        virtualDisplay = null
        imageReader = null
        projection = null
        running = false
        instance = null
        HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"stopped"}""")
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
            nm?.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.mirror_channel), NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(text: String): Notification {
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_linkdeck)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .addAction(0, getString(R.string.mirror_stop), stopPi)
            .setOngoing(true)
            .build()
    }
}
