package com.shilapi.xcertplay.media

import org.concentus.OpusDecoder

/** Platform-independent Opus decoder for head units whose Android codec list omits Opus. */
internal class SoftwareOpusDecoder(sampleRate: Int, private val channels: Int) {
    private val decoder = OpusDecoder(sampleRate, channels)
    private val samples = ShortArray(MAX_FRAME_SAMPLES * channels)
    val pcm = ByteArray(samples.size * 2)

    /** Decodes one raw Opus access unit and returns the number of valid bytes in [pcm]. */
    fun decode(packet: ByteArray): Int {
        val decodedSamples = decoder.decode(
            packet,
            0,
            packet.size,
            samples,
            0,
            MAX_FRAME_SAMPLES,
            false,
        )
        val sampleCount = decodedSamples * channels
        for (index in 0 until sampleCount) {
            val value = samples[index].toInt()
            pcm[index * 2] = value.toByte()
            pcm[index * 2 + 1] = (value shr 8).toByte()
        }
        return sampleCount * 2
    }

    private companion object {
        const val MAX_FRAME_SAMPLES = 5_760
    }
}
