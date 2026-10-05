package com.shilapi.xcertplay

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import com.shilapi.xcertplay.host.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en", shadows = [FileProviderPathTestShadow::class])
class DiagnosticExportUiTest {
    @Test fun missingPickerSavesAReportAndProvidesSelectableTextInsideDiPlay() {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        val activity = controller.get()
        val context = activity.applicationContext
        val authority = "${context.packageName}.diagnostic-reports"
        val info = context.packageManager.resolveContentProvider(authority, PackageManager.GET_META_DATA)!!
        ShadowContentResolver.registerProviderInternal(authority, DiagnosticReportProvider().apply { attachInfo(context, info) })
        val missingPicker = object : ActivityResultLauncher<String>() {
            override fun launch(input: String, options: ActivityOptionsCompat?) {
                throw ActivityNotFoundException("No DocumentsUI")
            }
            override fun unregister() = Unit
            override fun getContract() = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
        }
        ReflectionHelpers.setField(activity, "export", missingPicker)
        try {
            ReflectionHelpers.callInstanceMethod<Unit>(activity, "chooseReportDestination")
            val fallback = requireNotNull(ShadowAlertDialog.getLatestAlertDialog())
            assertTrue(descendants(fallback.window!!.decorView).filterIsInstance<TextView>()
                .any { it.text == activity.getString(R.string.report_picker_unavailable_hint) })
            assertFalse(ReflectionHelpers.getField<Boolean>(activity, "exportInProgress"))
            fallback.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            val deadline = System.nanoTime() + 5_000_000_000L
            while (ShadowAlertDialog.getLatestAlertDialog() === fallback && System.nanoTime() < deadline) {
                Thread.sleep(20)
                shadowOf(Looper.getMainLooper()).idle()
            }
            val saved = requireNotNull(ShadowAlertDialog.getLatestAlertDialog())
            val reports = java.io.File(context.getExternalFilesDir(null)!!, "diagnostic-reports")
            val file = reports.listFiles()!!.single()
            assertTrue(descendants(saved.window!!.decorView).filterIsInstance<TextView>()
                .any { it.text.contains(file.absolutePath) })
            saved.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val viewer = ShadowAlertDialog.getLatestAlertDialog()
            assertTrue(descendants(viewer.window!!.decorView).filterIsInstance<TextView>()
                .any { it.isTextSelectable && it.text.contains("Android 9 / API 28") })
            viewer.dismiss()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test @Config(sdk = [30]) fun theSingleSaveActionOpensThePickerOnAndroidEleven() {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        val activity = controller.get()
        var launches = 0
        val picker = object : ActivityResultLauncher<String>() {
            override fun launch(input: String, options: ActivityOptionsCompat?) {
                assertTrue(input.endsWith(".txt"))
                launches++
            }
            override fun unregister() = Unit
            override fun getContract() = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
        }
        try {
            ReflectionHelpers.setField(activity, "export", picker)
            ReflectionHelpers.setField(activity, "page", "settings")
            ReflectionHelpers.callInstanceMethod<Unit>(activity, "render")
            val buttons = descendants(activity.window.decorView).filterIsInstance<android.widget.Button>().toList()
            val save = buttons.single { it.text == activity.getString(R.string.save_diagnostic_report) }
            assertFalse(buttons.any { it.text == activity.getString(R.string.upload_report_to_cloud) ||
                it.text == activity.getString(R.string.choose_save_location) })
            save.performClick()
            save.performClick()
            assertEquals(1, launches)
            assertFalse(ReflectionHelpers.getField<Boolean>(activity, "exportInProgress"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }
}
