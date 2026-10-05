package com.shilapi.xcertplay.media

import android.util.Log
import org.concentus.OpusApplication
import org.concentus.OpusEncoder as ConcentusOpusEncoder
import java.io.Closeable

/** Encodes 20 ms chunks of 48 kHz mono PCM for the wireless CarPlay microphone uplink. */
internal class OpusEncoder(bitrate: Int) : Closeable {
    private val encoder: ConcentusOpusEncoder? = try {
        ConcentusOpusEncoder(SAMPLE_RATE, CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP).apply {
            setBitrate(bitrate)
        }.also {
            Log.i(TAG, "Opus microphone software encoder started bitrate=$bitrate name=Concentus")
        }
    } catch (error: Exception) {
        Log.w(TAG, "Opus microphone software encoder unavailable", error)
        null
    }
    private val output = ByteArray(MAX_OUTPUT_BYTES)
    private var closed = false
    private var outputPackets = 0

    val available: Boolean get() = encoder != null && !closed

    fun encode(pcm: ByteArray): List<ByteArray> {
        val encoder = encoder ?: return emptyList()
        if (closed || pcm.size % (CHANNELS * 2) != 0) return emptyList()
        val frameSamples = pcm.size / (CHANNELS * 2)
        val size = try {
            encoder.encode(pcm, 0, frameSamples, output, 0, output.size)
        } catch (error: Exception) {
            Log.w(TAG, "Opus microphone software encode failed", error)
            return emptyList()
        }
        if (size <= 0) return emptyList()
        val packet = output.copyOf(size)
        outputPackets++
        if (outputPackets <= FIRST_PACKET_LOG_COUNT) {
            Log.i(
                TAG,
                "Opus microphone packet=$outputPackets bytes=${packet.size} " +
                    "head=${packet.copyOf(minOf(packet.size, 16)).toHexString()}",
            )
        }
        return listOf(packet)
    }

    override fun close() {
        closed = true
    }

    private companion object {
        const val TAG = "xcertplay-usb"
        const val SAMPLE_RATE = 48_000
        const val CHANNELS = 1
        const val MAX_OUTPUT_BYTES = 1_275
        const val FIRST_PACKET_LOG_COUNT = 3
    }
}
