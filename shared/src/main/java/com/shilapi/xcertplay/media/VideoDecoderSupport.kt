package com.shilapi.xcertplay.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/** Use the same decoder ordering for display negotiation and playback. */
object VideoDecoderSupport {
    fun isHardware(info: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isHardwareAccelerated
        else !info.name.startsWith("OMX.google.", true) &&
            !info.name.startsWith("OMX.ffmpeg.", true) &&
            !info.name.startsWith("c2.android.", true)

    fun supportsRate(info: MediaCodecInfo, mime: String, width: Int, height: Int, fps: Int): Boolean =
        runCatching {
            info.getCapabilitiesForType(mime).videoCapabilities
                ?.areSizeAndRateSupported(width, height, fps.toDouble()) == true
        }.getOrDefault(false)

    fun candidates(mime: String, width: Int, height: Int, fps: Int): List<MediaCodecInfo> =
        runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mime, true) } &&
                    runCatching {
                        info.getCapabilitiesForType(mime).videoCapabilities?.isSizeSupported(width, height) == true
                    }.getOrDefault(false)
            }.sortedWith(compareBy<MediaCodecInfo> { !isHardware(it) }
                .thenBy { !supportsRate(it, mime, width, height, fps) })
        }.getOrDefault(emptyList())
}
