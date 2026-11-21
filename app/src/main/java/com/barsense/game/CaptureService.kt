package com.barsense.game

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = "barsense_capture"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "BarSense screen analysis", NotificationManager.IMPORTANCE_LOW))
            startForeground(7, Notification.Builder(this, channel).setContentTitle("BarSense active").setContentText("Screen analysis session").setSmallIcon(android.R.drawable.ic_menu_view).build())
        }
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        val dm = resources.displayMetrics
        reader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay("BarSenseCapture", dm.widthPixels, dm.heightPixels, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
        reader?.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try { analyze(image) } finally { image.close() }
        }, null)
        return START_NOT_STICKY
    }
    private fun analyze(image: Image) {
        // Initial deterministic detector: count red-dominant pixels by column.
        // This is diagnostic only; automatic jumping is intentionally disabled until
        // egg localization and gap timing are calibrated against live gameplay.
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val w = image.width; val h = image.height
        val step = 8
        var redHits = 0
        var y = h / 4
        while (y < h * 3 / 4) {
            var x = 0
            while (x < w) {
                val i = y * rowStride + x * pixelStride
                if (i + 2 < buffer.limit()) {
                    val rr = buffer.get(i).toInt() and 255
                    val gg = buffer.get(i+1).toInt() and 255
                    val bb = buffer.get(i+2).toInt() and 255
                    if (rr > 130 && rr > gg * 1.35 && rr > bb * 1.25) redHits++
                }
                x += step
            }
            y += step
        }
        BarSenseState.lastRedPixelSamples = redHits
    }
    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop(); super.onDestroy()
    }
}
object BarSenseState { @Volatile var lastRedPixelSamples: Int = 0 }
