package com.shilapi.xcertplay.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.Closeable

/** Opt-in, one-shot A2DP-sink handoff. Preserves any later user-initiated reconnection. */
@SuppressLint("MissingPermission")
internal class GeelyBluetoothAudioGuard(
    context: Context,
    private val address: String,
    private val report: (String) -> Unit,
) : Closeable {
    private val app = context.applicationContext
    private val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
    private var proxy: BluetoothProfile? = null
    private var closed = false
    private var attempted = false

    fun start() {
        if (Build.VERSION.SDK_INT >= 31 && app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            report("Factory Bluetooth audio handoff unavailable: connection permission")
            return
        }
        try {
            val requested = adapter?.getProfileProxy(app, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, connected: BluetoothProfile) = synchronized(this@GeelyBluetoothAudioGuard) {
                    if (closed) { adapter?.closeProfileProxy(profile, connected); return@synchronized }
                    proxy = connected
                    disconnectPeer()
                }
                override fun onServiceDisconnected(profile: Int) = synchronized(this@GeelyBluetoothAudioGuard) { proxy = null }
            }, A2DP_SINK) == true
            if (!requested) { report("Factory Bluetooth audio handoff unavailable: A2DP sink"); close() }
        } catch (error: Exception) {
            report("Factory Bluetooth audio handoff unavailable: ${error.javaClass.simpleName}")
            close()
        }
    }

    @Synchronized
    private fun disconnectPeer() {
        if (closed || attempted) return
        val profile = proxy ?: return
        attempted = true
        try {
            val device = profile.connectedDevices.firstOrNull { it.address.equals(address, true) }
            if (device == null) { report("Factory Bluetooth one-shot handoff: peer not connected"); return }
            val disconnected = profile.javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(profile, device)
            report("Factory Bluetooth one-shot handoff accepted=${disconnected == true}")
        } catch (error: Exception) {
            report("Factory Bluetooth music handoff unavailable: ${error.javaClass.simpleName}")
        } finally {
            close()
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        proxy?.let { runCatching { adapter?.closeProfileProxy(A2DP_SINK, it) } }
        proxy = null
    }

    companion object {
        private const val A2DP_SINK = 11
    }
}
