package com.shilapi.xcertplay

import android.net.Uri

/** Builds a reviewable issue draft; never sends a report or holds a GitHub credential. */
internal object GitHubIssueReport {
    const val REPOSITORY = "https://github.com/CoderLin99/DiPlay"
    const val MAX_DESCRIPTION_LENGTH = 2_000
    const val MAX_URL_LENGTH = 7_500
    const val BUILD = "0.2.11-geely.4"

    data class Draft(val url: String, val report: String)

    fun prepare(fileName: String, description: String, report: String): Draft {
        require(fileName.matches(Regex("DiPlay-[0-9]{8}-[0-9]{6}-[0-9]{3}\\.txt")))
        require(description.isNotBlank() && description.length <= MAX_DESCRIPTION_LENGTH)
        val safeReport = report.lineSequence().mapNotNull(DiagnosticRedactor::redact).joinToString("\n")
        var summary = takeCharacters(description.trim(), 500)
        var excerpt = takeCharacters(safeReport, 1_200)
        fun url(): String = Uri.parse("$REPOSITORY/issues/new").buildUpon()
            .appendQueryParameter("template", "geely-bug.md")
            .appendQueryParameter("title", "[Bug] ${takeCharacters(description.lineSequence().first(), 50)}")
            .appendQueryParameter("body", buildString {
                appendLine("### Problem / 故障描述\n$summary\n")
                appendLine("### Build\n$BUILD\n")
                appendLine("### Diagnostics / 诊断摘要")
                excerpt.lineSequence().forEach { appendLine("    $it") }
                appendLine("\n### Full report / 完整日志")
                appendLine("Attach / 请附上：$fileName")
                appendLine("The summary above may be shortened. Review the full report before submitting.")
                appendLine("摘要可能有截断。请检查日志，添加完整附件后提交。")
            }).build().toString()
        var result = url()
        while (result.length > MAX_URL_LENGTH) {
            if (excerpt.isNotEmpty()) excerpt = takeCharacters(excerpt, excerpt.codePointCount(0, excerpt.length) / 2)
            else summary = takeCharacters(summary, summary.codePointCount(0, summary.length) / 2)
            result = url()
        }
        return Draft(result, "Problem / 故障描述:\n${description.trim()}\n\n$safeReport\n")
    }

    private fun takeCharacters(value: String, limit: Int): String =
        value.substring(0, value.offsetByCodePoints(0, minOf(limit, value.codePointCount(0, value.length))))
}
