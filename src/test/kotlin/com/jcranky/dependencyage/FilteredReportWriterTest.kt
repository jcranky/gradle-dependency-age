package com.jcranky.dependencyage

import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FilteredReportWriterTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `unverifiable candidates get their own section and are not recommended as safe`() {
        val outputFile = File.createTempFile("report-filtered", ".txt")

        FilteredReportWriter(clock).write(
            outputFile = outputFile,
            accepted = listOf(CandidateUpdate("com.example", "safe", "1.0.0", "2.0.0")),
            rejected = emptyList(),
            unverified = listOf(CandidateUpdate("com.example", "mystery", "1.0.0", "2.0.0")),
            minAgeDays = 7,
        )

        val text = outputFile.readText()
        assertTrue("## Accepted candidates" in text && "- com.example:safe:1.0.0 -> 2.0.0" in text)
        assertTrue("## Unverifiable candidates" in text)
        assertTrue("# UNVERIFIED: com.example:mystery:1.0.0 -> 2.0.0 (publication date unknown)" in text)
        assertFalse(
            """- com\.example:mystery:1\.0\.0 -> 2\.0\.0""".toRegex().containsMatchIn(text),
            "unverifiable candidates must not appear in the accepted recommendations section:\n$text",
        )
        assertFalse(" # UNVERIFIED" in text, "annotation lines must start with the # marker, no leading space:\n$text")
    }

    @Test
    fun `rejected lines start with the FILTERED marker`() {
        val outputFile = File.createTempFile("report-filtered", ".txt")

        FilteredReportWriter(clock).write(
            outputFile = outputFile,
            accepted = emptyList(),
            rejected = listOf(RejectedCandidate(CandidateUpdate("com.example", "fresh", "1.0.0", "2.0.0"), clock.instant())),
            minAgeDays = 7,
        )

        val text = outputFile.readText()
        assertTrue("\n# FILTERED:" in text, "expected '# FILTERED:' at line start:\n$text")
        assertFalse(" # FILTERED" in text, "annotation lines must not have a leading space:\n$text")
    }

    @Test
    fun `candidates published less than a day ago are not reported as 0 days`() {
        val outputFile = File.createTempFile("report-filtered", ".txt")

        FilteredReportWriter(clock).write(
            outputFile = outputFile,
            accepted = emptyList(),
            rejected = listOf(
                RejectedCandidate(CandidateUpdate("com.example", "hours-old", "1.0.0", "2.0.0"), clock.instant().minus(java.time.Duration.ofHours(3))),
            ),
            minAgeDays = 7,
        )

        val text = outputFile.readText()
        assertTrue("(published less than a day ago)" in text, "sub-day ages should read 'less than a day':\n$text")
        assertFalse("(published 0 days ago)" in text, "sub-day ages must not be reported as 0 days:\n$text")
    }
}
