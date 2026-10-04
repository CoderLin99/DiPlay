package com.shilapi.xcertplay

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GitHubIssueReportTest {
    private val name = "DiPlay-20261004-120000-123.txt"

    @Test fun reportTargetsOurIssueFormAndFiltersCredentials() {
        val draft = GitHubIssueReport.prepare(name, "高德没有声音", "Android 9\npassword=secret-value\npeer=10.0.0.12\nMAC aa:bb:cc:dd:ee:ff")
        val uri = Uri.parse(draft.url)
        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertEquals("/CoderLin99/DiPlay/issues/new", uri.path)
        assertEquals("geely-bug.md", uri.getQueryParameter("template"))
        assertFalse(draft.report.contains("secret-value"))
        assertFalse(draft.report.contains("10.0.0.12"))
        assertFalse(draft.report.contains("aa:bb:cc:dd:ee:ff"))
        assertTrue(uri.getQueryParameter("body")!!.contains(name))
        assertTrue(draft.report.contains("高德没有声音"))
    }

    @Test fun largeUnicodeReportsKeepFullAttachmentAndBoundBrowserUrl() {
        val description = "导航失声🚗".repeat(150)
        val report = "frame arrived 地图🚗\n".repeat(8_000)
        val draft = GitHubIssueReport.prepare(name, description, report)
        assertTrue(draft.url.length <= GitHubIssueReport.MAX_URL_LENGTH)
        assertTrue(draft.report.contains(description))
        assertTrue(draft.report.length > 100_000)
        assertFalse(Uri.parse(draft.url).getQueryParameter("body")!!.contains('\uFFFD'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathInAttachmentName() {
        GitHubIssueReport.prepare("../report.txt", "problem", "report")
    }
}
