package com.shilapi.xcertplay.media

import android.media.AudioDeviceInfo
import android.media.AudioManager
import org.json.JSONObject

/** A saved output preference. Device IDs alone may change when the audio service restarts. */
data class AudioOutputDevice(val id: Int, val type: Int, val address: String, val name: String) {
    fun resolve(manager: AudioManager?): AudioDeviceInfo? {
        val outputs = outputs(manager)
        return if (address.isNotBlank()) outputs.firstOrNull { it.type == type && it.address == address }
        else outputs.firstOrNull { it.id == id && it.type == type && it.productName.toString() == name }
    }

    fun encode(): String = JSONObject().put("id", id).put("type", type)
        .put("address", address).put("name", name).toString()

    companion object {
        fun outputs(manager: AudioManager?): List<AudioDeviceInfo> =
            runCatching { manager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.toList().orEmpty() }
                .getOrDefault(emptyList())

        fun from(device: AudioDeviceInfo) = AudioOutputDevice(
            device.id, device.type, device.address, device.productName.toString(),
        )

        fun decode(value: String?): AudioOutputDevice? = runCatching {
            if (value == null) null else JSONObject(value).let {
                AudioOutputDevice(it.getInt("id"), it.getInt("type"), it.getString("address"), it.getString("name"))
            }
        }.getOrNull()
    }
}
