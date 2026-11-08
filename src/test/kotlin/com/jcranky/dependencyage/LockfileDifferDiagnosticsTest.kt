package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LockfileDifferDiagnosticsTest {
    @Test
    fun `readFromBase returns Failure when working dir is not a git repo`(
        @TempDir tempDir: Path,
    ) {
        val notARepo = tempDir.toFile()
        File(notARepo, "gradle.lockfile").writeText("com.example:lib:1.0.0=runtimeClasspath\n")
        File(notARepo, "buildscript-gradle.lockfile").writeText("")

        val result =
            LockfileDiffer.readFromBase(
                baseRef = "HEAD",
                mainLockfile = File(notARepo, "gradle.lockfile"),
                buildscriptLockfile = File(notARepo, "buildscript-gradle.lockfile"),
                workingDir = notARepo,
            )

        assertTrue(
            result is LockfileDiffer.ReadFromBaseResult.Failure,
            "expected Failure when not in a git repo, got $result",
        )
        val failure = result as LockfileDiffer.ReadFromBaseResult.Failure
        assertTrue(
            failure.reason.contains("git") || failure.reason.contains("repo"),
            "expected reason to mention git or repo, got: ${failure.reason}",
        )
    }
}
