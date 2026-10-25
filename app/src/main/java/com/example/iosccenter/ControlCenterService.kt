package com.example.iosccenter

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.iosccenter.ui.ControlCenterPanel

class ControlCenterService : AccessibilityService() {

    companion object {
        const val ACTION_SHOW = "com.example.iosccenter.ACTION_SHOW"
        private const val HOT_ZONE_HEIGHT_DP = 32
        private const val SWIPE_THRESHOLD_DP = 24
    }

    private var panelRoot: View? = null
    private var hotZone: View? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = show()
    }

    override fun onServiceConnected() {
        val filter = IntentFilter(ACTION_SHOW)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        addHotZone()
    }

    private fun addHotZone() {
        if (hotZone != null) return
        val wm = getSystemService(WindowManager::class.java)
        val dm = resources.displayMetrics
        val view = object : View(this) {
            private var startY = 0f
            private var fired = false
            override fun onTouchEvent(e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { startY = e.rawY; fired = false }
                    MotionEvent.ACTION_MOVE ->
                        if (!fired && e.rawY - startY > SWIPE_THRESHOLD_DP * dm.density) {
                            fired = true
                            show()
                        }
                }
                return true
            }
        }
        val lp = WindowManager.LayoutParams(
            dm.widthPixels / 2,
            (HOT_ZONE_HEIGHT_DP * dm.density).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END }
        wm.addView(view, lp)
        hotZone = view
    }

    private fun show() {
        if (panelRoot != null) return
        val wm = getSystemService(WindowManager::class.java)
        val panel = ControlCenterPanel(this) { hide() }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 31) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            lp.blurBehindRadius = 40
        }
        wm.addView(panel.root, lp)
        panelRoot = panel.root
    }

    private fun hide() {
        panelRoot?.let { getSystemService(WindowManager::class.java).removeView(it) }
        panelRoot = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        hide()
        hotZone?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        hotZone = null
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }
}
