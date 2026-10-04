package com.shilapi.xcertplay

import android.content.Context
import android.media.AudioManager

/** Read-only head-unit routing state. This cannot observe WeChat's recording route on the iPhone. */
internal object AudioRoutingSnapshot {
    @Suppress("DEPRECATION")
    fun capture(context: Context): String = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return "audioManager=unavailable"
        buildString {
            appendLine("headUnit mode=${audio.mode} micMuted=${audio.isMicrophoneMute} " +
                "bluetoothSco=${audio.isBluetoothScoOn} speakerphone=${audio.isSpeakerphoneOn} musicActive=${audio.isMusicActive}")
            appendLine("mediaVolume=${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
            appendLine("visibleRecordingConfigurations=${runCatching { audio.activeRecordingConfigurations.size }.getOrNull()}")
            for ((label, flags) in listOf("inputs" to AudioManager.GET_DEVICES_INPUTS, "outputs" to AudioManager.GET_DEVICES_OUTPUTS)) {
                appendLine("$label=" + audio.getDevices(flags).joinToString { "id:${it.id}/type:${it.type}" })
            }
            appendLine("iphoneRecordingRoute=not_observable_from_head_unit")
        }
    }.getOrElse { "audioRouting unavailable=${it.javaClass.simpleName}" }
}
