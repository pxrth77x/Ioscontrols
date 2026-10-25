package com.example.iosccenter.hook

import android.content.Intent
import android.view.MotionEvent
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SystemUiHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "IOSCC"
        private const val APP_PKG = "com.example.iosccenter"
        private const val ACTION_SHOW = "com.example.iosccenter.ACTION_SHOW"
        private val CANDIDATES = listOf(
            "com.android.systemui.statusbar.phone.PhoneStatusBarView"
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        for (name in CANDIDATES) {
            val cls = XposedHelpers.findClassIfExists(name, lpparam.classLoader) ?: continue
            runCatching {
                XposedHelpers.findAndHookMethod(
                    cls, "onTouchEvent", MotionEvent::class.java, TouchHook()
                )
                XposedBridge.log("$TAG: hooked $name")
            }.onFailure { XposedBridge.log("$TAG: failed $name: $it") }
        }
    }

    private class TouchHook : XC_MethodHook() {
        private var tracking = false
        private var startY = 0f

        override fun beforeHookedMethod(p: MethodHookParam) {
            val v = p.thisObject as View
            val e = p.args[0] as MotionEvent
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tracking = e.x > v.width * 0.6f
                    startY = e.y
                }
                MotionEvent.ACTION_MOVE ->
                    if (tracking && e.y - startY > 24 * v.resources.displayMetrics.density) {
                        tracking = false
                        v.context.sendBroadcast(Intent(ACTION_SHOW).setPackage(APP_PKG))
                        p.result = true
                    }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> tracking = false
            }
        }
    }
}
