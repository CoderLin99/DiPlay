package com.shilapi.xcertplay.vehicle

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaRecorder
import android.os.Build
import com.shilapi.xcertplay.airplay.AirPlayIcon
import org.json.JSONObject
import java.io.File

/** Public routing and artwork from the installed Geely CarPlay configuration. */
class GeelyFactoryCarPlay private constructor(private val config: JSONObject?) {
    val manufacturer: String = config?.optJSONObject("ManufacturerInfo")
        ?.optString("Manufacturer")?.takeIf { it.isNotBlank() } ?: "Geely"
    val model: String = config?.optJSONObject("ManufacturerInfo")
        ?.optString("Model")?.takeIf { it.isNotBlank() } ?: "Geely Design"
    val iconLabel: String = config?.optJSONObject("OemIconInfo")
        ?.optString("OemIconLabel")?.takeIf { it.isNotBlank() } ?: "Geely"

    fun icons(): List<AirPlayIcon> = listOf(104, 120, 180, 256).mapNotNull { size ->
        val name = "icon_${size}x$size.png"
        val configured = config?.optJSONObject("OemIconInfo")?.optString("icon_${size}x$size")
        val file = File(configured?.takeIf { it.isNotBlank() } ?: "$DIRECTORY/$name")
        runCatching {
            // Only artwork in the factory directory is eligible; never read identity files.
            if (file.canonicalFile.parentFile != File(DIRECTORY).canonicalFile) return@runCatching null
            val bytes = readBounded(file, MAX_ICON_BYTES) ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outMimeType != "image/png" || bounds.outWidth != size || bounds.outHeight != size) return@runCatching null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
            bitmap.recycle()
            AirPlayIcon(size, size, bytes)
        }.getOrNull()
    }

    fun audioUsage(kind: String, fallback: Int): Int {
        val name = "AUDIO_USAGE_CP_$kind"
        frameworkValue(AudioAttributes::class.java, name)?.let { return it }
        val value = config?.optJSONObject("AudioAttrs")?.optJSONObject("AudioUsage")?.optInt(name, -1)
        // The APK's template maps every usage to MEDIA. Do not turn calls/guidance into music.
        return value?.takeIf { it > 0 && (kind == "MEDIA" || it != AudioAttributes.USAGE_MEDIA) } ?: fallback
    }

    fun microphoneSource(audioType: String, sampleRate: Int, wireless: Boolean): Int? {
        val kind = when (audioType.lowercase()) {
            "speechrecognition" -> "SIRI"
            "telephony" -> when {
                sampleRate <= 8_000 -> "PHONE_NB"
                sampleRate <= 16_000 -> "PHONE_WB"
                sampleRate <= 24_000 -> "PHONE_SWB"
                else -> "PHONE_FB"
            }
            "facetime" -> "FACETIME"
            else -> return null
        }
        val name = "AUDIO_SOURCE_${if (wireless) "WIRELESS_" else ""}CP_$kind"
        return frameworkValue(MediaRecorder.AudioSource::class.java, name)
            ?: config?.optJSONObject("AudioAttrs")?.optJSONObject("AudioSource")?.optInt(name, -1)
                ?.takeIf { it > MediaRecorder.AudioSource.MIC }
    }

    companion object {
        private const val DIRECTORY = "/vendor/etc/carplay"
        private const val MAX_ICON_BYTES = 512 * 1024
        private val factory by lazy {
            val json = runCatching {
                readBounded(File("$DIRECTORY/carplay_config.json"), 64 * 1024)
                    ?.let { bytes ->
                        val raw = JSONObject(bytes.toString(Charsets.UTF_8))
                        JSONObject().apply {
                            // Keep only the public sections used here, never accessory identities.
                            listOf("ManufacturerInfo", "OemIconInfo", "AudioAttrs").forEach { name ->
                                raw.optJSONObject(name)?.let { put(name, it) }
                            }
                        }
                    }
            }.getOrNull()
            GeelyFactoryCarPlay(json)
        }

        fun load(context: Context): GeelyFactoryCarPlay? {
            val identity = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL} ${Build.PRODUCT} ${Build.DEVICE}"
            val modelMatches = Regex("(?i)(?:^|[^a-z0-9])(?:G636|FX11|KX11|E245|Geely)(?:$|[^a-z0-9])").containsMatchIn(identity)
            val configMatches = factory.config?.optJSONObject("ManufacturerInfo")
                ?.optString("Manufacturer")?.equals("Geely", true) == true
            val factoryReceiverInstalled = runCatching {
                context.packageManager.getApplicationInfo("com.autolink.carplay", 0)
            }.isSuccess
            val factoryUiOrServiceInstalled = listOf(
                "com.autolink.carplay.app",
                "com.geely.service.oneosapi",
            ).any { packageName ->
                runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
            }
            val installed = factoryReceiverInstalled && factoryUiOrServiceInstalled
            return factory.takeIf { modelMatches || configMatches || installed }
        }

        private fun frameworkValue(type: Class<*>, name: String): Int? =
            runCatching { type.getField(name).getInt(null) }.getOrNull()?.takeIf { it > 0 }

        private fun readBounded(file: File, limit: Int): ByteArray? =
            if (file.isFile && file.length() in 1..limit.toLong()) {
                file.inputStream().use { it.readBytes() }.takeIf { it.size <= limit }
            } else null
    }
}
