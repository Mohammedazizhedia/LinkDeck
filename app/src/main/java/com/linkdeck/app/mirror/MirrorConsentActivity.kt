package com.linkdeck.app.mirror

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.linkdeck.core.network.HostBridge

/**
 * HOST side trampoline: launched from a heads-up notification when the
 * controller asks to mirror. Starts the system MediaProjection consent flow,
 * then forwards the result to ScreenCaptureService.
 */
class MirrorConsentActivity : Activity() {

    companion object {
        private const val REQ_PROJECTION = 1001

        fun request(context: Context) {
            val intent = Intent(context, MirrorConsentActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        runCatching { startActivityForResult(pm.createScreenCaptureIntent(), REQ_PROJECTION) }
            .onFailure {
                HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"error","data":"projection_unavailable"}""")
                finish()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PROJECTION && data != null) {
            val service = Intent(this, ScreenCaptureService::class.java)
                .setAction(ScreenCaptureService.ACTION_START)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            startForegroundService(service)
        } else {
            HostBridge.hostServer?.broadcast("mirror_event", """{"kind":"error","data":"projection_denied"}""")
        }
        finish()
    }
}
