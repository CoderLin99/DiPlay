package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

internal data class SteeringObservedKey(
    val keyCode: Int,
    val event: Int,
    val source: String,
    val logTag: String = "",
    val broadcastAction: String = "",
    val keyExtra: String = "",
    val eventExtra: String = "",
    val logContains: List<String> = emptyList(),
)

/** DiPlay owns both the system-log stream and receivers for actions discovered in that stream. */
internal class SteeringKeyLogMonitor(
    context: Context,
    private val bindings: List<SteeringBinding>,
    private val discovering: Boolean,
    private val onKey: (SteeringObservedKey) -> Unit,
) : AutoCloseable {
    private val app = context.applicationContext
    @Volatile private var closed = false
    @Volatile private var process: Process? = null
    private val receivers = ConcurrentHashMap<String, BroadcastReceiver>()
    private val lastPulse = ConcurrentHashMap<String, Long>()
    private val recentLines = ArrayDeque<String>()

    fun start() {
        bindings.filter { it.source == "broadcast" }.forEach { subscribe(it.broadcastAction) }
        if (!discovering && bindings.none { it.source == "logcat" }) return
        Thread({
            while (!closed) {
              try {
                val startedAt = System.currentTimeMillis()
                val reader = ProcessBuilder("logcat", "-v", "threadtime", "-v", "epoch", "-T", "1")
                    .redirectErrorStream(true).start()
                process = reader
                if (closed) { reader.destroy(); return@Thread }
                reader.inputStream.bufferedReader().use { lines ->
                    while (!closed) {
                        val line = lines.readLine() ?: break
                        if (line.length > 4096) continue
                        val header = epochLine.find(line) ?: continue
                        val time = (header.groupValues[1].toDoubleOrNull()?.times(1000))?.toLong() ?: continue
                        if (time < startedAt) continue
                        val tag = header.groupValues[2].trim()
                        if (tag.startsWith("DiPlay-")) continue
                        val message = header.groupValues[3]
                        if (diagnosticKeyLine.containsMatchIn(tag + " " + message)) synchronized(this) {
                            if (recentLines.size >= 30) recentLines.removeFirst()
                            recentLines.addLast("$tag: ${message.take(512)}")
                        }
                        val name = broadcastName.findAll(message).map { it.groupValues[1] }
                            .firstOrNull { it.contains("key", true) || it.contains("steering", true) ||
                                message.contains("key", true) || tag.contains("key", true) || it == Intent.ACTION_MEDIA_BUTTON }.orEmpty()
                        if (discovering && name.isNotBlank()) subscribe(name)
                        val rules = if (discovering) emptyList() else bindings.filter {
                            it.source == "logcat" && it.logTag == tag && it.logContains.isNotEmpty() && it.logContains.all { part -> message.contains(part) }
                        }
                        if (rules.isNotEmpty()) {
                            rules.forEach { onKey(SteeringObservedKey(it.keyCode, it.event, "logcat", tag, logContains = it.logContains)) }
                            continue
                        }
                        parseKey(message, tag, name)?.let(onKey)
                    }
                }
              } catch (error: Exception) {
                if (!closed) Log.w("DiPlay-KeyLogs", "System broadcast log stream unavailable", error)
              } finally { process?.destroy(); process = null }
              if (!closed) runCatching { Thread.sleep(3_000L) }
            }
        }, "diplay-system-key-logs").start()
    }

    @Synchronized private fun subscribe(action: String) {
        if (closed || action.isBlank() || action.length > 160 || !action.matches(Regex("[A-Za-z0-9_.]+")) ||
            receivers.containsKey(action) || receivers.size >= 16) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (closed || intent?.action != action) return
                readBroadcast(intent)?.let(onKey)
            }
        }
        try {
            ContextCompat.registerReceiver(app, receiver, IntentFilter(action), ContextCompat.RECEIVER_EXPORTED)
            receivers[action] = receiver
            if (discovering) onKey(SteeringObservedKey(0, -1, "broadcast", broadcastAction = action))
        } catch (error: Exception) { Log.w("DiPlay-KeyLogs", "System key broadcast registration unavailable", error) }
    }

    @Suppress("DEPRECATION")
    private fun readBroadcast(intent: Intent): SteeringObservedKey? = runCatching {
        val standard = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (standard != null) {
            if (standard.repeatCount > 0 || standard.action !in 0..1) return@runCatching null
            return@runCatching SteeringObservedKey(standard.keyCode, standard.action, "broadcast",
                broadcastAction = intent.action.orEmpty(), keyExtra = Intent.EXTRA_KEY_EVENT)
        }
        val configured = bindings.filter { it.source == "broadcast" && it.broadcastAction == intent.action }
        val fields = (configured.map { it.keyExtra } + listOf("keyCode", "keycode", "key_code", "KEY_CODE", "key", "rawKeyCode", "raw"))
            .filter(String::isNotBlank).distinct()
        val extras = intent.extras ?: return@runCatching null
        val keyField = fields.firstOrNull { extras.containsKey(it) && integer(extras.get(it)) != null } ?: return@runCatching null
        val code = integer(extras.get(keyField)) ?: return@runCatching null
        if (code !in 1..1_000_000) return@runCatching null
        val eventField = (configured.map { it.eventExtra } + listOf(
            "keyEvent", "key_event", "keyAction", "key_action", "event", "action", "ACTION",
        ))
            .filter(String::isNotBlank).distinct().firstOrNull { extras.containsKey(it) && eventValue(extras.get(it)) != null }
        if ((integer(extras.get("repeatCount")) ?: 0) > 0) return@runCatching null
        val event = eventField?.let { eventValue(extras.get(it)) } ?: 2
        if (eventField == null) {
            val pulse = intent.action.orEmpty() + ":" + code
            val time = SystemClock.elapsedRealtime()
            val previous = lastPulse.put(pulse, time)
            if (previous != null && time - previous < 150L) return@runCatching null
        }
        SteeringObservedKey(code, event, "broadcast", broadcastAction = intent.action.orEmpty(),
            keyExtra = keyField, eventExtra = eventField.orEmpty())
    }.getOrNull()

    @Synchronized override fun close() {
        closed = true; process?.destroy()
        receivers.values.forEach { runCatching { app.unregisterReceiver(it) } }
        receivers.clear()
    }

    @Synchronized fun diagnostics(): String = "logStream=${process != null} broadcasts=" +
        receivers.keys.joinToString(",").ifBlank { "NONE" } + "\n" + recentLines.joinToString("\n")

    companion object {
        private val epochLine = Regex("^\\s*(\\d+\\.\\d+)\\s+\\d+\\s+\\d+\\s+[VDIWEFAS]\\s+([^:]+):\\s*(.*)$")
        private val broadcastName = Regex("(?i)\\b(?:act|broadcastAction|intentAction|action)[\"']?[\\s=:]+[\"']?([A-Za-z][A-Za-z0-9_.]+)")
        private val diagnosticKeyLine = Regex("(?i)key|button|steering|broadcast|input")
        private val raw = Regex("(?i)\\b(?:rawKeyCode|raw)[\"']?[\\s=:]+[\"']?(0x[0-9a-f]+|\\d+)\\b")
        private val key = Regex("(?i)\\b(?:keyCode|key_code|keyId|key_id|keyValue|key_value|key)[\"']?[\\s=:]+[\"']?(0x[0-9a-f]+|\\d+)\\b")
        private val keyFunction = Regex("(?i)\\bonKey(Down|Up|Pressed|Released)\\s*[(:=]\\s*(?:keyCode\\s*[=:]\\s*)?(0x[0-9a-f]+|\\d+)\\b")
        private val functionEvent = Regex("(?i)\\bonKey(Down|Up|Pressed|Released)\\b")
        private val namedKey = Regex("(?i)\\bkeyCode[\"']?[\\s=:]+[\"']?(KEYCODE_[A-Z_0-9]+)")
        private val action = Regex("(?i)\\b(?:keyEvent|key_event|keyAction|key_action|action|event)[\"']?[\\s=:]+[\"']?(?:ACTION_)?(DOWN|UP|SINGLE|LONG|DOUBLE|[0-4])\\b")
        private val repeat = Regex("(?i)\\brepeat(?:Count)?[\"']?[\\s=:]+[\"']?(\\d+)")

        fun parseKey(message: String, tag: String, broadcastAction: String = ""): SteeringObservedKey? {
            if (message.length > 4096 || tag.isBlank() || (repeat.find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0) > 0) return null
            val numeric = raw.find(message) ?: key.find(message)
            val function = keyFunction.find(message)
            val number = integer(numeric?.groupValues?.get(1)) ?: integer(function?.groupValues?.get(2))
                ?: namedKey.find(message)?.groupValues?.get(1)?.let(KeyEvent::keyCodeFromString) ?: return null
            if (number !in 1..1_000_000) return null
            val event = eventValue(action.find(message)?.groupValues?.get(1)) ?:
                eventValue(functionEvent.find(message)?.groupValues?.get(1)) ?: -1
            return SteeringObservedKey(number, event, "logcat", tag, broadcastAction)
        }

        private fun integer(value: Any?): Int? = when (value) {
            is Byte -> value.toInt()
            is Short -> value.toInt()
            is Int -> value
            is Long -> value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
            is String -> value.trim().let { if (it.startsWith("0x", true)) it.substring(2).toIntOrNull(16) else it.toIntOrNull() }
            else -> null
        }

        private fun eventValue(value: Any?): Int? {
            val number = integer(value)
            if (number != null) return number.takeIf { it in 0..4 }
            return when (value?.toString()?.uppercase()?.removePrefix("ACTION_")) {
                "DOWN", "PRESSED" -> 0
                "UP", "RELEASED" -> 1
                "SINGLE" -> 2
                "LONG" -> 3
                "DOUBLE" -> 4
                else -> null
            }
        }
    }
}
