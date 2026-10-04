package com.shilapi.xcertplay

import android.os.Build
import android.os.Looper
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.orchestration.CarPlayRuntimeConfig
import com.shilapi.xcertplay.orchestration.CarPlayTransport
import com.shilapi.xcertplay.orchestration.MfiTarget
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE, shadows = [GeelySessionCompatibilityTest.Handoff::class])
class GeelySessionCompatibilityTest {
    private val app = RuntimeEnvironment.getApplication()
    private val controllers = mutableListOf<CarPlayController>()
    private val sessions = mutableListOf<AirPlaySession>()
    private val originalModel = Build.MODEL
    private val log = mutableListOf<String>()
    private val media = object : AirPlayMediaHandler {}
    private val identity = AirPlayIdentity.generate()
    private val air = AirPlayConfig("test", "02:00:00:00:00:02", "02:00:00:00:00:01", "1",
        AirPlayDisplayConfig(800, 480))

    @After fun cleanup() {
        controllers.forEach { CarPlayMediaKeys.detach(it); it.close() }
        sessions.forEach { it.close() }
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", originalModel)
        Handoff.starts = 0
    }

    @Test fun videoConnectionAndPhoneHandoffDoNotDisconnectBluetoothByDefault() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "FX11_LOW")
        val controller = controller()
        ReflectionHelpers.setField(controller, "wirelessPeerBluetoothAddress", "02:00:00:00:00:01")
        val callbacks = ReflectionHelpers.getField<AirPlaySessionListener>(controller, "sessionListener")
        val session = session()
        callbacks.onSessionActive(session)
        callbacks.onCommand(session, "disableBluetooth", mapOf("deviceID" to "02:00:00:00:00:01"))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, Handoff.starts)
        assertTrue(log.any { it.contains("automatic disconnect disabled") })
    }

    @Test fun controlsExistBeforeMusicPacketsAndBluetoothModeDoesNotClaimThem() {
        val first = controller()
        CarPlayMediaKeys.attach(app, first, manageAudioFocus = true)
        CarPlayMediaKeys.onSessionConnected(first)
        assertTrue(CarPlayMediaKeys.steeringDiagnostics().contains("mediaSession=true"))
        assertTrue(CarPlayMediaKeys.steeringDiagnostics().contains("focusHeld=false"))
        val second = controller()
        CarPlayMediaKeys.attach(app, second, forwardMedia = false)
        CarPlayMediaKeys.onSessionConnected(second)
        CarPlayMediaKeys.onSessionDisconnected(first)
        CarPlayMediaKeys.onMediaAudioChanged(true)
        shadowOf(Looper.getMainLooper()).idle()
        val state = CarPlayMediaKeys.steeringDiagnostics()
        assertTrue(state.contains("mediaSession=false"))
        assertTrue(state.contains("connected=true"))
        assertTrue(state.contains("forwardMedia=false"))
    }

    private fun controller(): CarPlayController = CarPlayController(app,
        CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL, transport = CarPlayTransport.WIRELESS,
            identification = Iap2IdentificationConfig(name = "test", modelIdentifier = "test", manufacturer = "test",
                serialNumber = "test", firmwareVersion = "1", hardwareVersion = "1", carPlayUsbInterfaceNumber = 3)),
        air, identity, PairingStore(), object : AirPlaySessionListener {
            override fun onDebugLog(message: String) { log.add(message) }
        }, media, {}).also(controllers::add)

    private fun session() = AirPlaySession(Socket(), air, identity, PairingStore(), null,
        object : AirPlaySessionListener {}, media).also(sessions::add)

    @Implements(className = "com.shilapi.xcertplay.vehicle.GeelyBluetoothAudioGuard", isInAndroidSdk = false)
    class Handoff {
        @Implementation fun start() { starts++ }
        companion object { var starts = 0 }
    }
}
