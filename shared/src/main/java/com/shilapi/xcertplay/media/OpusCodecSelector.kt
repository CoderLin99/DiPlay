package com.shilapi.xcertplay.media

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

internal fun createOpusDecoder(): MediaCodec = createOpusCodec(encoder = false)

internal fun createOpusEncoder(): MediaCodec = createOpusCodec(encoder = true)

private fun createOpusCodec(encoder: Boolean): MediaCodec {
    if (Build.MODEL.orEmpty().contains("KX11", ignoreCase = true)) {
        val softwareCodec = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
            info.isEncoder == encoder &&
                info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, ignoreCase = true) } &&
                (info.name.startsWith("OMX.google.", ignoreCase = true) ||
                    info.name.startsWith("c2.android.", ignoreCase = true))
        }
        if (softwareCodec != null) {
            runCatching { MediaCodec.createByCodecName(softwareCodec.name) }.getOrNull()?.let { return it }
        }
    }
    return if (encoder) MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
    else MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
}
