package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** Manually requested, five-second, noninteractive marker. Never chooses a screen by size. */
internal object SecondaryDisplayProbe {
    private val handler = Handler(Looper.getMainLooper())
    private var manager: WindowManager? = null
    private var marker: View? = null
    private val history = ArrayDeque<String>()
    private val expire = Runnable { stop() }

    fun show(activity: Activity, id: Int): Boolean {
        stop()
        val display = activity.getSystemService(DisplayManager::class.java)?.getDisplay(id)
        if (display == null || id == Display.DEFAULT_DISPLAY || display.state == Display.STATE_OFF ||
            !Settings.canDrawOverlays(activity)) {
            record("probe id=$id result=unavailable overlay=${Settings.canDrawOverlays(activity)}")
            return false
        }
        val context = activity.createDisplayContext(display)
        val wm = context.getSystemService(WindowManager::class.java) ?: return false
        val view = object : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                paint.color = Color.CYAN
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRect(2f, 2f, width - 2f, height - 2f, paint)
                paint.style = Paint.Style.FILL
                paint.textSize = height * 0.45f
                canvas.drawText("ID $id", 8f, height * 0.68f, paint)
            }
        }
        val params = WindowManager.LayoutParams(
            minOf(240, (display.mode.physicalWidth / 5).coerceAtLeast(40)),
            minOf(80, (display.mode.physicalHeight / 5).coerceAtLeast(24)),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.RIGHT
            x = 12; y = 12
            title = "DiPlay display identification"
        }
        return try {
            wm.addView(view, params)
            manager = wm; marker = view
            handler.postDelayed(expire, 5_000L)
            record("probe id=$id result=window_added size=${params.width}x${params.height} visibleOnPanel=unconfirmed")
            true
        } catch (error: RuntimeException) {
            runCatching { wm.removeViewImmediate(view) }
            record("probe id=$id result=failed error=${error.javaClass.simpleName}")
            false
        }
    }

    fun stop() {
        handler.removeCallbacks(expire)
        marker?.let { view -> runCatching { manager?.removeViewImmediate(view) } }
        marker = null; manager = null
    }

    @Synchronized private fun record(line: String) {
        if (history.size >= 12) history.removeFirst()
        history.addLast(line)
    }

    @Synchronized fun diagnostics(context: Context): String = buildString {
        context.getSystemService(DisplayManager::class.java)?.displays.orEmpty().forEach {
            appendLine("display id=${it.displayId} label=${it.name} size=${it.mode.physicalWidth}x${it.mode.physicalHeight} " +
                "flags=${it.flags} state=${it.state} rotation=${it.rotation}")
        }
        history.forEach { appendLine(it) }
    }
}
