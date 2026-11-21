package com.barsense.game

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val captureRequest = 41
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,48,32,32) }
        layout.addView(TextView(this).apply { text = "BarSense • Game-specific helper\n\n1. Grant overlay permission\n2. Enable BarSense accessibility service\n3. Start screen capture\n4. Open your game and use the floating panel.\n\nDetection is experimental: red-bar geometry only. Egg tracking is not yet reliable across changing appearances."; textSize = 17f })
        layout.addView(Button(this).apply { text = "Grant overlay permission"; setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } })
        layout.addView(Button(this).apply { text = "Enable accessibility tapping"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        layout.addView(Button(this).apply { text = "Start BarSense overlay"; setOnClickListener {
            if (!Settings.canDrawOverlays(this@MainActivity)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return@setOnClickListener }
            startService(Intent(this@MainActivity, OverlayService::class.java))
        } })
        layout.addView(Button(this).apply { text = "Start screen analysis"; setOnClickListener {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mgr.createScreenCaptureIntent(), captureRequest)
        } })
        setContentView(layout)
    }
    @Deprecated("Deprecated by platform API but compatible with older Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val i = Intent(this, CaptureService::class.java).putExtra("resultCode", resultCode).putExtra("data", data)
            startForegroundService(i)
        }
    }
}
