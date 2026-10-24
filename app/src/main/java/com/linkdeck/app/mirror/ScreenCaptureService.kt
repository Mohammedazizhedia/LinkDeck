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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

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
            instance?.let {
                context.startService(Intent(context, ScreenCaptureService::class.java).setAction(ACTION_STOP))
            }
        }
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var frameJob: ScheduledFuture<*>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.mirror_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_linkdeck)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.mirror_running))
            .addAction(0, getString(R.string.mirror_stop), stopPi)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopCapture(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                val rc = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                @Suppress("DEPRECATION")
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (rc != 0 && data != null) startCapture(rc, data)
                else {
                    HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"error","data":"projection_denied"}""")
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
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        frameJob = scheduler.scheduleWithFixedDelay(::grabFrame, 0, 200, TimeUnit.MILLISECONDS)
        running = true
        instance = this
        HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"started"}""")
    }

    private fun grabFrame() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return
        try {
            val plane = image.planes[0]
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bmp = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888,
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
}
