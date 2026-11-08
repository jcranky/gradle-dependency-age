package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class DependencyUpdatesReportParserTest {
    @Test
    fun `parses candidate lines from dependencyUpdates report`() {
        val file = File.createTempFile("report", ".txt").also { it.deleteOnExit() }
        file.writeText(
            """
            ------------------------------------------------------------
            : Project Dependency Updates (report to plain text file)
            ------------------------------------------------------------

            The following dependencies have later milestone versions:
             - at.favre.lib:bcrypt:0.10.2 -> 0.11.0
             - com.zaxxer:HikariCP:7.1.0 -> 8.0.0
                declared in the 'runtimeClasspath' configuration
             - org.jetbrains.kotlin:kotlin-stdlib:2.4.10 -> 2.5.0

            The following dependencies have later stable versions:
            <none>

            Gradle release-candidate updates:
             - org.gradle:gradle:9.7 -> 9.8.0-rc-1
            """.trimIndent(),
        )

        val candidates = DependencyUpdatesReportParser.parse(file)

        assertEquals(
            listOf(
                CandidateUpdate("at.favre.lib", "bcrypt", "0.10.2", "0.11.0"),
                CandidateUpdate("com.zaxxer", "HikariCP", "7.1.0", "8.0.0"),
                CandidateUpdate("org.jetbrains.kotlin", "kotlin-stdlib", "2.4.10", "2.5.0"),
                CandidateUpdate("org.gradle", "gradle", "9.7", "9.8.0-rc-1"),
            ),
            candidates,
        )
    }

    @Test
    fun `parses bracket-format candidate lines from current versions plugin report`() {
        val file = File.createTempFile("report", ".txt").also { it.deleteOnExit() }
        file.writeText(
            """
            ------------------------------------------------------------
            : Project Dependency Updates (report to plain text file)
            ------------------------------------------------------------

            The following dependencies have later milestone versions:
             - ch.qos.logback:logback-classic [1.6.5 -> 1.6.6]
                 http://logback.qos.ch
             - io.github.ben-manes.versions:io.github.ben-manes.versions.gradle.plugin [0.66.0 -> 0.67.0]
                 contributed by a plugin

            The following dependencies are using the latest milestone version:
             - at.favre.lib:bcrypt:0.10.2
            """.trimIndent(),
        )

        val candidates = DependencyUpdatesReportParser.parse(file)

        assertEquals(
            listOf(
                CandidateUpdate("ch.qos.logback", "logback-classic", "1.6.5", "1.6.6"),
                CandidateUpdate(
                    "io.github.ben-manes.versions",
                    "io.github.ben-manes.versions.gradle.plugin",
                    "0.66.0",
                    "0.67.0",
                ),
            ),
            candidates,
        )
    }

    @Test
    fun `resolves a bracket-format version chain to the newest milestone version`() {
        val file = File.createTempFile("report", ".txt").also { it.deleteOnExit() }
        file.writeText(
            """
            The following dependencies have later milestone versions:
             - org.jetbrains.exposed:exposed-core [1.5.0 -> 1.5.1 -> 1.6.0]
            """.trimIndent(),
        )

        val candidates = DependencyUpdatesReportParser.parse(file)

        assertEquals(
            listOf(
                CandidateUpdate("org.jetbrains.exposed", "exposed-core", "1.5.0", "1.6.0"),
            ),
            candidates,
        )
    }

    @Test
    fun `returns empty list when report file does not exist`() {
        val candidates = DependencyUpdatesReportParser.parse(File("/nonexistent/path"))
        assertEquals(emptyList<CandidateUpdate>(), candidates)
    }

    @Test
    fun `returns empty list when report has no candidates`() {
        val file = File.createTempFile("report", ".txt").also { it.deleteOnExit() }
        file.writeText("The following dependencies have later milestone versions:\n - (none)\n")

        assertEquals(emptyList<CandidateUpdate>(), DependencyUpdatesReportParser.parse(file))
    }
}
