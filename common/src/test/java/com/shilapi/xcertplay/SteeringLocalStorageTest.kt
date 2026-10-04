package com.shilapi.xcertplay

import android.app.job.JobScheduler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SteeringLocalStorageTest {
    @Test fun savingMappingsDoesNotScheduleNetworkUpload() {
        val context = RuntimeEnvironment.getApplication()
        val profile = SteeringProfile("Boyue L 2023", "FX11", firmware = "Galaxy OS 2.5.0",
            bindings = listOf(SteeringBinding("next", 200087, 0, source = "oneos")))
        SteeringProfiles.save(context, profile)
        assertEquals(profile, SteeringProfiles.loadEnabled(context))
        assertFalse(context.getSystemService(JobScheduler::class.java).allPendingJobs.any { it.id == 0x535750 })
        assertFalse(java.io.File(context.filesDir, "steering-profiles/outbox").exists())
    }
}
