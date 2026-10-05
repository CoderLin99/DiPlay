package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.IInterface
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import android.util.Log

internal data class GeelySteeringKeyEvent(
    val keyCode: Int,
    val rawKeyCode: Int,
    val action: Int,
    val eventTimeMs: Long,
) {
    companion object {
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_SINGLE = 2
        const val ACTION_LONG = 3
        const val ACTION_DOUBLE = 4
    }
}

/**
 * Passive listener for the OneOS steering input service used by Geely head units.
 * The listener never asks OneOS to intercept a key, so the vehicle's normal key handling remains active.
 */
internal class GeelySteeringWheelInputChannel(
    context: Context,
    private val onEvent: (GeelySteeringKeyEvent) -> Unit,
) : AutoCloseable {
    companion object {
        private const val TAG = "DiPlay-GeelyWheel"
        private const val ONE_OS_PACKAGE = "com.geely.service.oneosapi"
        private const val ONE_OS_SERVICE = "$ONE_OS_PACKAGE.OneOSApiService"
        private const val SERVICE_MANAGER_DESCRIPTOR = "com.geely.lib.oneosapi.IServiceManager"
        private const val INPUT_MANAGER_DESCRIPTOR = "com.geely.lib.oneosapi.input.IInputManager"
        private const val INPUT_LISTENER_DESCRIPTOR = "com.geely.lib.oneosapi.input.IInputListener"
        private const val INPUT_SERVICE_TYPE = 8
        private const val TRANSACTION_GET_SERVICE = 2
        private const val TRANSACTION_REGISTER = 3
        private const val TRANSACTION_UNREGISTER = 4
        private const val CONNECT_RETRY_MS = 2_000L

        fun isKnownGeelyHeadUnit(): Boolean = Build.MODEL.orEmpty().uppercase().let { model ->
            model.contains("G636") || model.contains("FX11") || model.contains("KX11")
        }

        fun enabledByDefault(): Boolean = isKnownGeelyHeadUnit() &&
            (!Build.MODEL.orEmpty().contains("KX11", ignoreCase = true) ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    }

    private val app = context.applicationContext
    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recentEvents = ArrayDeque<String>()
    private val eventDecoder = GeelySteeringEventDecoder(
        record = { line -> synchronized(lock) {
            if (recentEvents.size >= 32) recentEvents.removeFirst()
            recentEvents.addLast(line)
        } },
        emit = onEvent,
    )
    @Volatile private var registeredKeys = intArrayOf()
    private var serviceManager: IBinder? = null
    private var inputManager: IBinder? = null
    private var bound = false
    private var enabled = false
    private var closed = false
    private var lastFailure = "NONE"
    private var serviceMetadata = "NOT_CHECKED"

    private val retryInputService = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (closed || !enabled || inputManager != null) return
                inputManager = serviceManager?.let(::getInputService)
                if (inputManager != null) applyRequestedKeysLocked()
                else if (bound) mainHandler.postDelayed(this, CONNECT_RETRY_MS)
            }
        }
    }

    private val retryConnection = object : Runnable {
        override fun run() {
            synchronized(lock) {
                if (closed || !enabled || inputManager != null) return
                unbindLocked()
                bindLocked()
            }
        }
    }

    private val listener = object : Binder(), IInterface {
        init { attachInterface(this, INPUT_LISTENER_DESCRIPTOR) }

        override fun asBinder(): IBinder = this

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(INPUT_LISTENER_DESCRIPTOR)
                return true
            }
            if (code !in 1..6) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(INPUT_LISTENER_DESCRIPTOR)
            val rawKeyCode = data.readInt()
            val action = if (code == 1) data.readInt() else -1
            data.readInt() // OneOS soft-key function.
            synchronized(lock) {
                if (enabled && !closed) eventDecoder.accept(code, rawKeyCode, action, SystemClock.elapsedRealtime())
            }
            reply?.writeNoException()
            return true
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(lock) {
                mainHandler.removeCallbacks(retryConnection)
                serviceManager = service
                inputManager = service?.let(::getInputService)
                if (inputManager == null) {
                    mainHandler.removeCallbacks(retryInputService)
                    mainHandler.postDelayed(retryInputService, CONNECT_RETRY_MS)
                } else {
                    applyRequestedKeysLocked()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) = resetConnection()
        override fun onBindingDied(name: ComponentName?) = resetConnection()
        override fun onNullBinding(name: ComponentName?) = resetConnection()
    }

    fun setEnabled(value: Boolean) {
        synchronized(lock) {
            if (closed || enabled == value) return
            enabled = value
            if (!enabled) {
                mainHandler.removeCallbacks(retryInputService)
                mainHandler.removeCallbacks(retryConnection)
                releaseRegisteredLocked()
                unbindLocked()
            } else {
                bindLocked()
                applyRequestedKeysLocked()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            enabled = false
            mainHandler.removeCallbacks(retryInputService)
            mainHandler.removeCallbacks(retryConnection)
            releaseRegisteredLocked()
            unbindLocked()
        }
    }

    fun diagnostics(): String = synchronized(lock) {
        "oneOs enabled=$enabled bound=$bound input=${inputManager != null} registered=${registeredKeys.size} " +
            "service=$serviceMetadata failure=$lastFailure" + "\n" + recentEvents.joinToString("\n")
    }

    fun ready(): Boolean = synchronized(lock) { registeredKeys.isNotEmpty() }

    private fun bindLocked() {
        if (closed || !enabled || bound) return
        serviceMetadata = runCatching {
            @Suppress("DEPRECATION")
            val info = app.packageManager.getServiceInfo(ComponentName(ONE_OS_PACKAGE, ONE_OS_SERVICE), 0)
            "enabled=${info.enabled && info.applicationInfo.enabled},exported=${info.exported},permission=${info.permission ?: "NONE"}"
        }.getOrElse { it.javaClass.simpleName }
        bound = runCatching {
            app.bindService(
                Intent().setClassName(ONE_OS_PACKAGE, ONE_OS_SERVICE),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }.onFailure {
            lastFailure = "bind ${it.javaClass.simpleName}: ${it.message?.take(160)}"
            Log.w(TAG, "OneOS input service bind failed", it)
        }.getOrDefault(false)
        if (!bound && lastFailure == "NONE") lastFailure = "service_not_bound"
        if (!bound) mainHandler.postDelayed(retryConnection, CONNECT_RETRY_MS)
    }

    private fun resetConnection() {
        synchronized(lock) {
            serviceManager = null
            inputManager = null
            registeredKeys = intArrayOf()
            mainHandler.removeCallbacks(retryInputService)
            mainHandler.removeCallbacks(retryConnection)
            if (!closed && enabled) mainHandler.postDelayed(retryConnection, CONNECT_RETRY_MS)
        }
    }

    private fun unbindLocked() {
        if (bound) runCatching { app.unbindService(connection) }
        bound = false
        serviceManager = null
        inputManager = null
        registeredKeys = intArrayOf()
    }

    private fun getInputService(manager: IBinder): IBinder? = transact(
        manager,
        TRANSACTION_GET_SERVICE,
        SERVICE_MANAGER_DESCRIPTOR,
        write = { writeInt(INPUT_SERVICE_TYPE) },
        read = { readStrongBinder() },
    )

    private fun applyRequestedKeysLocked() {
        val manager = inputManager ?: return
        if (!enabled || registeredKeys.isNotEmpty()) return
        val keyTable = GeelySteeringKeyCodes.oneOsListenerCodes()
        val registered = transact<Unit>(
            manager,
            TRANSACTION_REGISTER,
            INPUT_MANAGER_DESCRIPTOR,
            write = {
                writeStrongBinder(listener)
                writeString(app.packageName)
                writeIntArray(keyTable)
            },
            read = { Unit },
        ) != null
        if (registered) {
            registeredKeys = keyTable
            lastFailure = "NONE"
        }
    }

    private fun releaseRegisteredLocked() {
        val manager = inputManager
        if (manager != null && registeredKeys.isNotEmpty()) {
            transact<Unit>(
                manager,
                TRANSACTION_UNREGISTER,
                INPUT_MANAGER_DESCRIPTOR,
                write = {
                    writeStrongBinder(listener)
                    writeString(app.packageName)
                },
                read = { Unit },
            )
        }
        registeredKeys = intArrayOf()
    }

    private fun <T> transact(
        binder: IBinder,
        code: Int,
        descriptor: String,
        write: Parcel.() -> Unit,
        read: Parcel.() -> T,
    ): T? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(descriptor)
            data.write()
            if (!binder.transact(code, data, reply, 0)) {
                lastFailure = "transaction=$code unsupported"
                return null
            }
            reply.readException()
            reply.read()
        } catch (error: Throwable) {
            lastFailure = "transaction=$code ${error.javaClass.simpleName}: ${error.message?.take(160)}"
            Log.w(TAG, "OneOS input transaction failed", error)
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}

internal object GeelySteeringKeyCodes {
    const val MEDIA_PLAY_PAUSE = 200_085
    const val MEDIA_NEXT = 200_087
    const val MEDIA_PREVIOUS = 200_088
    const val VOICE_ASSIST = 200_231
    const val SEEK_NEXT = 210_005
    const val SEEK_PREVIOUS = 210_006

    fun canonicalize(keyCode: Int): Int? = when (keyCode) {
        MEDIA_PLAY_PAUSE, 85 -> MEDIA_PLAY_PAUSE
        MEDIA_NEXT, 87, 110_005 -> if (keyCode == 87) MEDIA_NEXT else if (keyCode == 110_005) SEEK_NEXT else MEDIA_NEXT
        MEDIA_PREVIOUS, 88, 110_006 -> if (keyCode == 88) MEDIA_PREVIOUS else if (keyCode == 110_006) SEEK_PREVIOUS else MEDIA_PREVIOUS
        VOICE_ASSIST, 231 -> VOICE_ASSIST
        SEEK_NEXT -> SEEK_NEXT
        SEEK_PREVIOUS -> SEEK_PREVIOUS
        else -> null
    }

    /** OneOS dispatches reliably with the full stock listener table; this listener stays passive. */
    fun oneOsListenerCodes(): IntArray = ((0..283).toList() + intArrayOf(
        110_001, 110_002, 110_003, 110_004, 110_005, 110_006, 110_007, 110_008, 110_009, 110_010,
        200_003, 200_004, 200_005, 200_006, 200_024, 200_025, 200_082, 200_085, 200_087, 200_088,
        200_110, 200_164, 200_176, 200_231, 200_400, 200_600,
        210_001, 210_002, 210_003, 210_004, 210_005, 210_006, 210_007, 210_008, 210_009, 210_010,
        210_020, 300_001, 300_002, 300_003, 300_005, 300_010, 300_020, 300_021, 300_022, 300_023,
        300_030, 300_031,
    ).toList()).distinct().toIntArray()
}
