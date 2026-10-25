package com.example.iosccenter

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.iosccenter.ui.ControlCenterPanel

class ControlCenterService : AccessibilityService() {

    companion object {
        const val ACTION_SHOW = "com.example.iosccenter.ACTION_SHOW"
    }

    private var panelRoot: View? = null

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
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }
}
