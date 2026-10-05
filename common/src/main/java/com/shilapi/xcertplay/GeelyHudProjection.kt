package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.shilapi.xcertplay.hud.CarPlayHudGuidance
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.min

internal data class GeelyHudDisplay(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
)

/** CarPlay maneuver projection for a Geely head unit's secondary HUD display. */
internal object GeelyHudProjection : DisplayManager.DisplayListener {
    private const val TAG = "DiPlay-GeelyHud"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activityRef: WeakReference<Activity>? = null
    private var displayManager: DisplayManager? = null
    private var windowManager: WindowManager? = null
    private var hudView: GeelyHudGuidanceView? = null
    private var attachedDisplayId = Display.INVALID_DISPLAY
    private var attachedWidth = 0
    private var attachedHeight = 0
    private var guidance: CarPlayHudGuidance? = null
    @Volatile private var windowStatus = "not_attached"
    private val expireGuidance = Runnable {
        guidance = null
        detachWindow()
    }

    fun attach(activity: Activity) {
        mainHandler.post {
            if (activityRef?.get() !== activity) {
                detachWindow()
                displayManager?.unregisterDisplayListener(this)
                activityRef = WeakReference(activity)
                displayManager = activity.getSystemService(DisplayManager::class.java)
                displayManager?.registerDisplayListener(this, mainHandler)
            }
            refresh()
        }
    }

    fun detach(activity: Activity) {
        mainHandler.post {
            if (activityRef?.get() !== activity) return@post
            detachWindow()
            displayManager?.unregisterDisplayListener(this)
            displayManager = null
            activityRef = null
        }
    }

    fun update(value: CarPlayHudGuidance?) {
        mainHandler.post {
            mainHandler.removeCallbacks(expireGuidance)
            guidance = value
            if (value != null) mainHandler.postDelayed(expireGuidance, 30_000L)
            refresh()
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        AirPlayPersistence.saveGeelyHudEnabled(context, enabled)
        if (enabled && context is Activity && !Settings.canDrawOverlays(context)) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }.onFailure { Log.w(TAG, "Could not open HUD display permission settings", it) }
        }
        mainHandler.post(::refresh)
    }

    fun availableDisplays(context: Context): List<GeelyHudDisplay> =
        context.getSystemService(DisplayManager::class.java)?.displays.orEmpty()
            .asSequence()
            .filter { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
            .map {
                val (width, height) = displaySize(context, it)
                GeelyHudDisplay(
                    id = it.displayId,
                    name = it.name,
                    width = width,
                    height = height,
                )
            }
            .sortedBy(GeelyHudDisplay::id)
            .toList()

    internal fun selectedIndex(displays: List<GeelyHudDisplay>, id: Int, name: String?): Int {
        val exact = displays.indexOfFirst { it.id == id }
        if (exact >= 0) return exact
        val matches = displays.indices.filter { displays[it].name == name }
        return matches.singleOrNull() ?: -1
    }

    fun selectDisplay(context: Context, display: GeelyHudDisplay?) {
        AirPlayPersistence.saveGeelyHudDisplay(
            context,
            display?.id ?: Display.INVALID_DISPLAY,
            display?.name,
        )
        mainHandler.post(::refresh)
    }

    fun setScale(context: Context, percent: Int) {
        AirPlayPersistence.saveGeelyHudScalePercent(context, percent)
        mainHandler.post {
            hudView?.scalePercent = percent
            refresh()
        }
    }

    fun diagnosticReport(context: Context): String {
        val selectedId = AirPlayPersistence.loadGeelyHudDisplayId(context)
        val selectedName = AirPlayPersistence.loadGeelyHudDisplayName(context).orEmpty()
        val displays = availableDisplays(context)
        return buildString {
            append("enabled=${AirPlayPersistence.loadGeelyHudEnabled(context)} ")
            append("overlayPermission=${Settings.canDrawOverlays(context)} ")
            append("selectedId=$selectedId selectedName=${selectedName.ifBlank { "automatic" }} ")
            append("attachedId=$attachedDisplayId guidancePresent=${guidance != null} windowStatus=$windowStatus")
            append("scale=${AirPlayPersistence.loadGeelyHudScalePercent(context)}% ")
            append("attachedId=$attachedDisplayId attachedSize=${attachedWidth}x$attachedHeight")
            appendLine()
            append("availableDisplays=")
            if (displays.isEmpty()) append("none") else append(
                displays.joinToString { "${it.id}:${it.name}:${it.width}x${it.height}" },
            )
        }
    }

    override fun onDisplayAdded(displayId: Int) = refresh()
    override fun onDisplayRemoved(displayId: Int) = refresh()
    override fun onDisplayChanged(displayId: Int) = refresh()

    private fun refresh() {
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            windowStatus = "host_unavailable"
            detachWindow()
            return
        }
        if (!AirPlayPersistence.loadGeelyHudEnabled(activity) ||
            guidance == null ||
            !Settings.canDrawOverlays(activity)
        ) {
            windowStatus = when {
                !AirPlayPersistence.loadGeelyHudEnabled(activity) -> "disabled"
                !Settings.canDrawOverlays(activity) -> "overlay_permission_missing"
                else -> "no_guidance"
            }
            detachWindow()
            return
        }
        val displays = displayManager?.displays
            ?.filter { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
            .orEmpty()
        val savedId = AirPlayPersistence.loadGeelyHudDisplayId(activity)
        val savedName = AirPlayPersistence.loadGeelyHudDisplayName(activity)
        val display = if (savedId != Display.INVALID_DISPLAY || savedName != null) {
            displays.firstOrNull { it.displayId == savedId }
                ?: displays.filter { it.name == savedName }.singleOrNull()
        } else {
            displays.firstOrNull { it.name.contains("hud", ignoreCase = true) }
                ?: displays.singleOrNull()
        }
        if (display == null) {
            windowStatus = "no_unique_target"
            detachWindow()
            return
        }
        val displayContext = activity.createDisplayContext(display)
        val (displayWidth, displayHeight) = displaySize(activity, display)
        if (attachedDisplayId != display.displayId ||
            attachedWidth != displayWidth || attachedHeight != displayHeight
        ) {
            detachWindow()
            val manager = displayContext.getSystemService(WindowManager::class.java) ?: return
            val view = GeelyHudGuidanceView(
                displayContext,
                AirPlayPersistence.loadGeelyHudScalePercent(activity),
            )
            val params = WindowManager.LayoutParams(
                displayWidth,
                displayHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "DiPlay HUD Navigation"
            }
            try {
                manager.addView(view, params)
                windowManager = manager
                hudView = view
                attachedDisplayId = display.displayId
                windowStatus = "window_added"
                attachedWidth = displayWidth
                attachedHeight = displayHeight
            } catch (error: RuntimeException) {
                windowStatus = "failed_${error.javaClass.simpleName}"
                Log.w(TAG, "Could not open the HUD display", error)
                runCatching { manager.removeViewImmediate(view) }
                return
            }
        }
        hudView?.guidance = guidance
    }

    private fun displaySize(context: Context, display: Display): Pair<Int, Int> {
        val metrics = context.createDisplayContext(display).resources.displayMetrics
        return (metrics.widthPixels.takeIf { it > 0 } ?: display.mode.physicalWidth) to
            (metrics.heightPixels.takeIf { it > 0 } ?: display.mode.physicalHeight)
    }

    private fun detachWindow() {
        val current = hudView
        hudView = null
        runCatching { current?.let { windowManager?.removeViewImmediate(it) } }
        windowManager = null
        attachedDisplayId = Display.INVALID_DISPLAY
        attachedWidth = 0
        attachedHeight = 0
    }
}

private class GeelyHudGuidanceView(context: Context, initialScalePercent: Int) : View(context) {
    var scalePercent = initialScalePercent
        set(value) {
            field = value
            invalidate()
        }

    var guidance: CarPlayHudGuidance? = null
        set(value) {
            field = value
            invalidate()
        }

    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(115, 255, 198)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val arrow = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val route = guidance ?: return
        val checkpoint = canvas.save()
        val contentScale = scalePercent / 100f
        canvas.scale(contentScale, contentScale, width * 0.5f, height * 0.5f)
        val heightScale = min(height.toFloat(), width * 0.45f)
        val centerY = height * 0.5f
        val iconSize = heightScale * 0.47f
        val left = width * 0.22f
        drawArrow(canvas, route.maneuverCode, left, centerY, iconSize)

        val distance = if (route.distanceMeters >= 1_000) {
            String.format(Locale.getDefault(), "%.1f km", route.distanceMeters / 1_000f)
        } else {
            String.format(Locale.getDefault(), "%d m", route.distanceMeters.coerceAtLeast(0))
        }
        val textLeft = width * 0.43f
        accent.textSize = heightScale * 0.19f
        canvas.drawText(distance, textLeft, centerY + accent.textSize * 0.18f, accent)
        if (route.road.isNotBlank()) {
            roadPaint.textSize = heightScale * 0.095f
            val maxWidth = (width - textLeft - width * 0.07f).coerceAtLeast(0f)
            val road = TextUtils.ellipsize(route.road, android.text.TextPaint(roadPaint), maxWidth, TextUtils.TruncateAt.END)
            canvas.drawText(road.toString(), textLeft, centerY + accent.textSize * 0.75f, roadPaint)
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun drawArrow(canvas: Canvas, maneuver: Int, x: Float, y: Float, size: Float) {
        arrow.reset()
        when (maneuver) {
            1, 2 -> {
                arrow.moveTo(18f, 36f); arrow.lineTo(18f, 0f)
                arrow.quadTo(18f, -20f, -2f, -20f); arrow.lineTo(-38f, -20f)
                arrow.moveTo(-22f, -34f); arrow.lineTo(-38f, -20f); arrow.lineTo(-22f, -6f)
            }
            3, 5 -> {
                arrow.moveTo(18f, 36f); arrow.lineTo(18f, 0f); arrow.lineTo(-28f, -36f)
                arrow.moveTo(-30f, -16f); arrow.lineTo(-28f, -36f); arrow.lineTo(-8f, -36f)
            }
            7, 8 -> {
                arrow.moveTo(22f, 36f); arrow.lineTo(22f, -8f)
                arrow.cubicTo(22f, -48f, -22f, -48f, -22f, -8f); arrow.lineTo(-22f, 16f)
                arrow.moveTo(-36f, 0f); arrow.lineTo(-22f, 16f); arrow.lineTo(-8f, 0f)
            }
            11 -> {
                arrow.moveTo(0f, 36f); arrow.lineTo(0f, -38f)
                arrow.moveTo(-14f, -22f); arrow.lineTo(0f, -38f); arrow.lineTo(14f, -22f)
            }
            else -> return
        }
        val checkpoint = canvas.save()
        canvas.translate(x, y)
        val scale = size / 88f
        canvas.scale(if (maneuver in listOf(2, 5, 8)) -scale else scale, scale)
        accent.style = Paint.Style.STROKE
        accent.strokeWidth = 9f
        accent.strokeCap = Paint.Cap.ROUND
        accent.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(arrow, accent)
        accent.style = Paint.Style.FILL
        canvas.restoreToCount(checkpoint)
    }
}
