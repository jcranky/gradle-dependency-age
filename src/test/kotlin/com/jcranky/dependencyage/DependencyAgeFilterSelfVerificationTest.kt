package com.jcranky.dependencyage

import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Dogfooding: runs the filter plugin through TestKit against a ben-manes
 * `dependencyUpdates` report staged in the synthetic project, with the
 * publish-time provider made deterministic and network-free via the same
 * cache-seeding trick as the verify dogfood (see
 * [DependencyAgeSelfVerificationTest]).
 */
class DependencyAgeFilterSelfVerificationTest {

    @field:TempDir
    lateinit var projectDir: File

    private val oldGav = "com.example.selfcheck:seeded-old-enough:2.0.0"
    private val youngGav = "com.example.selfcheck:seeded-too-young:3.0.0"

    @Test
    fun filterCheckAcceptsOldAndUnknownCandidatesAndRejectsTooYoungOnes() {
        setupProject()
        stageDependencyUpdatesReport()
        seedCache(oldGav, Instant.now().minus(100, ChronoUnit.DAYS))
        seedCache(youngGav, Instant.now())

        val result = GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
            .withArguments("filterCandidatesByAge")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":filterCandidatesByAge")!!.outcome)
        val filteredReport = File(projectDir, "build/dependencyUpdates/report-filtered.txt").readText()
        assertTrue("## Accepted candidates" in filteredReport, "unexpected report:\n$filteredReport")
        assertTrue(
            " - com.example.selfcheck:seeded-old-enough:1.0.0 -> 2.0.0" in filteredReport &&
                "seeded-old-enough" !in filteredReport.substringAfter("## Rejected candidates"),
            "old candidate should be accepted:\n$filteredReport",
        )
        assertTrue("## Rejected candidates" in filteredReport, "unexpected report:\n$filteredReport")
        assertTrue(
            "# FILTERED: com.example.selfcheck:seeded-too-young:1.0.0 -> 3.0.0" in filteredReport,
            "too-young candidate should be rejected:\n$filteredReport",
        )
    }

    private fun seedCache(gav: String, publishTime: Instant) {
        SelfVerificationSupport.seedPublishTimeCache(projectDir, mapOf(gav to publishTime))
    }

    private fun setupProject() {
        File(projectDir, "settings.gradle.kts")
            .writeText("rootProject.name = \"dependency-age-filter-selfcheck\"\n")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.jcranky.dependency-age-filter")
            }
            """.trimIndent() + "\n",
        )
    }

    private fun stageDependencyUpdatesReport() {
        File(projectDir, "build/dependencyUpdates").mkdirs()
        File(projectDir, "build/dependencyUpdates/report.txt").writeText(
            """
            The following dependencies have later milestone versions:
             - com.example.selfcheck:seeded-old-enough:1.0.0 -> 2.0.0
             - com.example.selfcheck:seeded-too-young:1.0.0 -> 3.0.0
             - com.example.selfcheck:seeded-unknown:1.0.0 -> 9.9.9
            """.trimIndent() + "\n",
        )
    }
}
