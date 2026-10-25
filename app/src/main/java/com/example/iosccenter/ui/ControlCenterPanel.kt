package com.example.iosccenter.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.iosccenter.Controls

fun Context.dp(v: Float) = v * resources.displayMetrics.density
fun Context.dpi(v: Float) = dp(v).toInt()

private fun rounded(color: Int, radius: Float) =
    GradientDrawable().apply { setColor(color); cornerRadius = radius }

private fun oval(color: Int) =
    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }

private const val GLASS = 0x55FFFFFF
private const val ACTIVE = 0xFF0A84FF.toInt()

class VerticalSlider(
    ctx: Context, private val icon: String, var progress: Float,
    private val onChange: (Float) -> Unit
) : View(ctx) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLASS }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = ctx.dp(24f); textAlign = Paint.Align.CENTER
    }
    private val clip = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = w / 2
        clip.reset(); clip.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)
        c.save(); c.clipPath(clip)
        c.drawRect(0f, 0f, w, h, bg)
        c.drawRect(0f, h * (1 - progress), w, h, fill)
        c.restore()
        c.drawText(icon, w / 2, h - context.dp(24f), iconPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
                progress = (1 - e.y / height).coerceIn(0f, 1f)
                onChange(progress); invalidate()
            }
        }
        return true
    }
}

class ControlCenterPanel(private val ctx: Context, private val dismiss: () -> Unit) {

    val root = FrameLayout(ctx)
    private val unit: Int
    private val gap = ctx.dpi(12f)

    init {
        val screenW = ctx.resources.displayMetrics.widthPixels
        val pad = ctx.dpi(16f)
        unit = (screenW - pad * 2 - gap * 3) / 4

        root.setBackgroundColor(0x44000000)
        root.setOnClickListener { dismiss() }
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        root.setOnKeyListener { _, code, ev ->
            if (code == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP) { dismiss(); true } else false
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, ctx.dpi(72f), pad, pad)
        }
        content.addView(row(connectivity(), media()))
        content.addView(row(
            leftColumn(),
            slider("☀", Controls.getBrightness(ctx)) { Controls.setBrightness(ctx, it) },
            slider("🔊", Controls.getVolume(ctx)) { Controls.setVolume(ctx, it) }
        ))
        content.addView(row(
            circle("🔦", false) { b -> b.isActivated = Controls.toggleTorch(ctx); style(b) },
            circle("🖥", false) { launch(Intent(Settings.ACTION_CAST_SETTINGS)) },
            circle("📷", false) { launch(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
            circle("🧮", false) { launch(ctx.packageManager.getLaunchIntentForPackage("com.google.android.calculator")) }
        ))
        root.addView(content)
    }

    private fun connectivity(): View {
        val grid = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(GLASS, ctx.dp(28f))
            setPadding(gap / 2, gap / 2, gap / 2, gap / 2)
        }
        fun pair(a: View, b: View) = LinearLayout(ctx).apply {
            addView(a); addView(b)
        }
        val u = (unit * 0.9f).toInt()
        fun t(icon: String, intent: Intent) = circle(icon, false, size = u) { launch(intent) }
        grid.addView(pair(
            t("✈", Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)),
            t("📶", Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        ))
        grid.addView(pair(
            t("Wi-Fi", Intent(Settings.Panel.ACTION_WIFI)),
            t("ᛒ", Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        ))
        return grid.sized(2 * unit + gap, 2 * unit + gap)
    }

    private fun media(): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(GLASS, ctx.dp(28f))
        }
        box.addView(TextView(ctx).apply { text = "Media"; setTextColor(Color.WHITE); textSize = 14f })
        val controls = LinearLayout(ctx).apply { gravity = Gravity.CENTER }
        fun key(label: String, code: Int) = TextView(ctx).apply {
            text = label; textSize = 26f; setTextColor(Color.WHITE)
            setPadding(ctx.dpi(10f), ctx.dpi(10f), ctx.dpi(10f), ctx.dpi(10f))
            setOnClickListener {
                val am = ctx.getSystemService(AudioManager::class.java)
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            }
        }
        controls.addView(key("⏮", KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        controls.addView(key("⏯", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        controls.addView(key("⏭", KeyEvent.KEYCODE_MEDIA_NEXT))
        box.addView(controls)
        return box.sized(2 * unit + gap, 2 * unit + gap)
    }

    private fun leftColumn(): View {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(ctx)
        top.addView(circle("🔒", !Controls.isRotationAuto(ctx)) { b ->
            b.isActivated = !Controls.toggleRotation(ctx); style(b)
        }.spaced())
        top.addView(circle("🔔", false) { launch(Intent(Settings.ACTION_SOUND_SETTINGS)) })
        col.addView(top)

        val focus = TextView(ctx).apply {
            text = "🌙  Focus"
            setTextColor(Color.WHITE); textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dpi(20f), 0, ctx.dpi(12f), 0)
            background = rounded(GLASS, unit / 2f)
            setOnClickListener {
                if (!Controls.hasDndAccess(ctx)) launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                else background = rounded(if (Controls.toggleDnd(ctx)) ACTIVE else GLASS, unit / 2f)
            }
        }.sized(2 * unit + gap, unit)
        (focus.layoutParams as LinearLayout.LayoutParams).topMargin = gap
        col.addView(focus)
        return col.sized(2 * unit + gap, 2 * unit + gap)
    }

    private fun slider(icon: String, start: Float, onChange: (Float) -> Unit): View =
        VerticalSlider(ctx, icon, start, onChange).sized(unit, 2 * unit + gap)

    private fun circle(
        icon: String, active: Boolean, size: Int = unit, onClick: (TextView) -> Unit
    ): TextView = TextView(ctx).apply {
        text = icon; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        isActivated = active
        style(this)
        setOnClickListener { onClick(this) }
        layoutParams = LinearLayout.LayoutParams(size, size)
    }

    private fun style(v: TextView) {
        v.background = oval(if (v.isActivated) ACTIVE else GLASS)
    }

    private fun TextView.spaced(): TextView {
        (layoutParams as LinearLayout.LayoutParams).rightMargin = gap
        return this
    }

    private fun View.sized(w: Int, h: Int): View {
        layoutParams = LinearLayout.LayoutParams(w, h); return this
    }

    private fun row(vararg views: View): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = gap }
            views.forEachIndexed { i, v ->
                addView(v)
                if (i < views.size - 1) (v.layoutParams as LinearLayout.LayoutParams).rightMargin = gap
            }
        }

    private fun launch(i: Intent?) {
        i ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
        dismiss()
    }
}
