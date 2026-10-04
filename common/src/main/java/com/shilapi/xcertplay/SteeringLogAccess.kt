package com.shilapi.xcertplay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.shilapi.xcertplay.adb.AdbKeys
import com.shilapi.xcertplay.adb.LocalAdb

/** Uses DiPlay's existing local car connection to grant access to its own log reader. */
internal object SteeringLogAccess {
    enum class Result { READY, APPROVAL_REQUIRED, UNAVAILABLE, DENIED }

    fun granted(context: Context) = context.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    /** Blocking; called only after the user chooses to allow button access. */
    fun request(context: Context): Result {
        if (granted(context)) return Result.READY
        return LocalAdb(AdbKeys.load(context)).use { adb ->
            when (adb.connect(mayAsk = true)) {
                LocalAdb.Access.NOT_APPROVED -> return@use Result.APPROVAL_REQUIRED
                LocalAdb.Access.UNREACHABLE, LocalAdb.Access.UNSUPPORTED -> return@use Result.UNAVAILABLE
                LocalAdb.Access.READY -> Unit
            }
            val name = context.packageName
            require(name.matches(Regex("[A-Za-z0-9_.]+")))
            adb.shell("pm grant $name android.permission.READ_LOGS")
            if (granted(context)) Result.READY else Result.DENIED
        }
    }
}
