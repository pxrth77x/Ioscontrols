package com.example.iosccenter

import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings

object Controls {
    private var torchOn = false

    fun toggleTorch(ctx: Context): Boolean {
        val cm = ctx.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        torchOn = !torchOn
        cm.setTorchMode(id, torchOn)
        return torchOn
    }

    fun canWriteSettings(ctx: Context) = Settings.System.canWrite(ctx)

    fun getBrightness(ctx: Context): Float =
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f

    fun setBrightness(ctx: Context, v: Float) {
        if (!canWriteSettings(ctx)) return
        Settings.System.putInt(
            ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
            (v * 255).toInt().coerceIn(1, 255)
        )
    }

    fun getVolume(ctx: Context): Float {
        val am = ctx.getSystemService(AudioManager::class.java)
        return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    fun setVolume(ctx: Context, v: Float) {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (v * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0
        )
    }

    fun isRotationAuto(ctx: Context) =
        Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1

    fun toggleRotation(ctx: Context): Boolean {
        if (!canWriteSettings(ctx)) return isRotationAuto(ctx)
        val next = if (isRotationAuto(ctx)) 0 else 1
        Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, next)
        return next == 1
    }

    fun hasDndAccess(ctx: Context) =
        ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun isDndOn(ctx: Context) =
        ctx.getSystemService(NotificationManager::class.java).currentInterruptionFilter !=
            NotificationManager.INTERRUPTION_FILTER_ALL

    fun toggleDnd(ctx: Context): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return false
        nm.setInterruptionFilter(
            if (isDndOn(ctx)) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
        return isDndOn(ctx)
    }
}
