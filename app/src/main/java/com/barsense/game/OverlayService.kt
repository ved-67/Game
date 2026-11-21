package com.barsense.game

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: LinearLayout? = null
    private var running = false
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) showPanel()
        return START_STICKY
    }
    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(14,10,14,10); setBackgroundColor(0xEE17172B.toInt())
        }
        val status = TextView(this).apply { text = "BarSense • READY"; setTextColor(-1) }
        p.addView(status)
        p.addView(Button(this).apply { text = "START AUTO"; setOnClickListener { running = true; status.text = "Detection ON • experimental" } })
        p.addView(Button(this).apply { text = "PAUSE"; setOnClickListener { running = false; status.text = "PAUSED" } })
        p.addView(Button(this).apply { text = "TAP"; setOnClickListener {
            val dm = resources.displayMetrics
            TapAccessibilityService.instance?.tap(dm.widthPixels * 0.89f, dm.heightPixels * 0.50f)
        } })
        p.addView(Button(this).apply { text = "CLOSE"; setOnClickListener { running = false; stopSelf() } })
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 12; y = 180 }
        wm.addView(p, params); panel = p
    }
    override fun onDestroy() { panel?.let { wm.removeView(it) }; panel = null; super.onDestroy() }
}
