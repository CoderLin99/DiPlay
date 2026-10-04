package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Use a window context on Android 11+, binding non-activity windows to their target display. */
internal fun secondaryOverlayContext(context: Context, display: Display): Context {
    val displayContext = context.applicationContext.createDisplayContext(display)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        displayContext.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else displayContext
}

/** Small persisted history: a reconnect/process restart must not erase the identification result. */
internal object SecondaryDisplayProbeHistory {
    private const val PREFS = "diplay_display_probes"
    @Synchronized fun record(context: Context, line: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val history = prefs.getString("history", "").orEmpty().lineSequence().filter { it.isNotBlank() }.toList()
        prefs.edit().putString("history", (history + "$stamp $line").takeLast(24).joinToString("\n")).apply()
    }
    fun report(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("history", null) ?: "No persisted probe attempts recorded by this version."
}

/** Manually requested, five-second, noninteractive marker. Never chooses a screen by size. */
internal object SecondaryDisplayProbe {
    private val handler = Handler(Looper.getMainLooper())
    private var manager: WindowManager? = null
    private var marker: View? = null
    private var probeContext: Context? = null
    private var targetId = Display.INVALID_DISPLAY
    private val expire = Runnable { stop("expired") }

    fun show(activity: Activity, id: Int): Boolean {
        stop("replaced")
        val app = activity.applicationContext
        fun record(line: String) = SecondaryDisplayProbeHistory.record(app, "probe id=$id $line")
        record("result=requested")
        return try {
            val display = activity.getSystemService(DisplayManager::class.java)?.getDisplay(id)
            if (display == null || id == Display.DEFAULT_DISPLAY || display.state == Display.STATE_OFF ||
                !Settings.canDrawOverlays(activity)) {
                record("result=unavailable overlay=${Settings.canDrawOverlays(activity)}")
                return false
            }
            val context = secondaryOverlayContext(activity, display)
            val wm = context.getSystemService(WindowManager::class.java)
            if (wm == null) { record("result=no_window_manager"); return false }
            val view = object : View(context) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                private var drawn = false
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    record("result=attached actualDisplay=${this.display?.displayId}")
                }
                override fun onDraw(canvas: Canvas) {
                    if (!drawn) {
                        drawn = true
                        record("result=drawn actualDisplay=${this.display?.displayId} size=${width}x$height visibleOnPanel=unconfirmed")
                    }
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
            // Store before addView, so partial attachment can also be cleaned up on failure.
            manager = wm; marker = view; probeContext = app; targetId = id
            wm.addView(view, params)
            handler.postDelayed(expire, 5_000L)
            record("result=window_added size=${params.width}x${params.height} windowContext=${Build.VERSION.SDK_INT >= 30} visibleOnPanel=unconfirmed")
            true
        } catch (error: RuntimeException) {
            record("result=failed error=${error.javaClass.simpleName}")
            stop("failed")
            false
        }
    }

    fun stop(reason: String = "manual") {
        handler.removeCallbacks(expire)
        marker?.let { view ->
            val removed = runCatching { manager?.removeViewImmediate(view) }.isSuccess
            probeContext?.let { SecondaryDisplayProbeHistory.record(it, "probe id=$targetId result=removed reason=$reason success=$removed") }
        }
        marker = null; manager = null; probeContext = null; targetId = Display.INVALID_DISPLAY
    }

    fun diagnostics(context: Context): String = buildString {
        context.getSystemService(DisplayManager::class.java)?.displays.orEmpty().forEach {
            appendLine("display id=${it.displayId} label=${it.name} size=${it.mode.physicalWidth}x${it.mode.physicalHeight} " +
                "flags=${it.flags} state=${it.state} rotation=${it.rotation}")
        }
        appendLine(SecondaryDisplayProbeHistory.report(context))
    }
}
