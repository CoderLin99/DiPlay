package com.shilapi.xcertplay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.shilapi.xcertplay.adb.AdbKeys
import com.shilapi.xcertplay.adb.LocalAdb

/** Uses DiPlay's existing local car connection to grant access to its own log reader. */
internal object SteeringLogAccess {
    enum class Result { READY, APPROVAL_REQUIRED, UNAVAILABLE, DENIED }
    @Volatile private var lastAttempt = "NOT_REQUESTED"

    fun diagnostics(): String = "keyLogAuthorization=$lastAttempt"

    fun granted(context: Context) = context.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    /** Blocking; called only after the user chooses to allow button access. */
    fun request(context: Context): Result {
        return runCatching { requestAccess(context) }.getOrElse { error ->
            lastAttempt = "error=${error.javaClass.simpleName}: ${error.message?.take(160)}"
            android.util.Log.w("DiPlay-KeyAccess", "Automatic key log authorization failed", error)
            Result.UNAVAILABLE
        }
    }

    private fun requestAccess(context: Context): Result {
        if (granted(context)) {
            lastAttempt = "ALREADY_GRANTED"
            return Result.READY
        }
        return LocalAdb(AdbKeys.load(context)).use { adb ->
            val access = adb.connect(mayAsk = true)
            lastAttempt = "adb=$access"
            when (access) {
                LocalAdb.Access.NOT_APPROVED -> return@use Result.APPROVAL_REQUIRED
                LocalAdb.Access.UNREACHABLE, LocalAdb.Access.UNSUPPORTED -> return@use Result.UNAVAILABLE
                LocalAdb.Access.READY -> Unit
            }
            val name = context.packageName
            require(name.matches(Regex("[A-Za-z0-9_.]+")))
            val output = adb.shell("pm grant $name android.permission.READ_LOGS")
            val result = if (granted(context)) Result.READY else if (output == null) Result.UNAVAILABLE else Result.DENIED
            lastAttempt = "adb=$access grant=$result"
            result
        }
    }
}
