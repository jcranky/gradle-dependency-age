package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LockfileDifferReadFromBaseTest {
    @Test
    fun `readFromBase returns GAVs from lockfile at base ref`(
        @TempDir tempDir: Path,
    ) {
        val repo = tempDir.toFile()
        val lockfile = File(repo, "gradle.lockfile")
        runGit(repo, "init")
        runGit(repo, "config", "user.email", "test@example.com")
        runGit(repo, "config", "user.name", "Test")
        lockfile.writeText(
            """
            com.example:old:1.0.0=runtimeClasspath
            com.example:shared:2.0.0=runtimeClasspath
            """.trimIndent() + "\n",
        )
        runGit(repo, "add", "gradle.lockfile")
        runGit(repo, "commit", "-m", "initial")
        val baseSha = runGit(repo, "rev-parse", "HEAD").trim()

        val result =
            LockfileDiffer.readFromBase(
                baseRef = baseSha,
                mainLockfile = lockfile,
                buildscriptLockfile = File(repo, "buildscript-gradle.lockfile"),
                workingDir = repo,
            )

        assertEquals(
            LockfileDiffer.ReadFromBaseResult.Success(
                listOf(
                    Gav("com.example", "old", "1.0.0"),
                    Gav("com.example", "shared", "2.0.0"),
                ),
            ),
            result,
        )
    }

    @Test
    fun `readFromBase returns empty Success when lockfile does not exist at base ref`(
        @TempDir tempDir: Path,
    ) {
        val repo = tempDir.toFile()
        runGit(repo, "init")
        runGit(repo, "config", "user.email", "test@example.com")
        runGit(repo, "config", "user.name", "Test")
        File(repo, "README.md").writeText("hello")
        runGit(repo, "add", "README.md")
        runGit(repo, "commit", "-m", "initial")
        val baseSha = runGit(repo, "rev-parse", "HEAD").trim()

        val result =
            LockfileDiffer.readFromBase(
                baseRef = baseSha,
                mainLockfile = File(repo, "gradle.lockfile"),
                buildscriptLockfile = File(repo, "buildscript-gradle.lockfile"),
                workingDir = repo,
            )

        assertEquals(
            LockfileDiffer.ReadFromBaseResult.Success(emptyList()),
            result,
        )
    }

    @Test
    fun `readFromBase returns Failure when base ref is blank`(
        @TempDir tempDir: Path,
    ) {
        val repo = tempDir.toFile()

        val result =
            LockfileDiffer.readFromBase(
                baseRef = "",
                mainLockfile = File(repo, "gradle.lockfile"),
                buildscriptLockfile = File(repo, "buildscript-gradle.lockfile"),
                workingDir = repo,
            )

        assertTrue(result is LockfileDiffer.ReadFromBaseResult.Failure)
        assertTrue((result as LockfileDiffer.ReadFromBaseResult.Failure).reason.contains("blank"))
    }

    @Test
    fun `readFromBase reads buildscript lockfile when present`(
        @TempDir tempDir: Path,
    ) {
        val repo = tempDir.toFile()
        runGit(repo, "init")
        runGit(repo, "config", "user.email", "test@example.com")
        runGit(repo, "config", "user.name", "Test")
        File(repo, "gradle.lockfile").writeText("com.example:lib:1.0.0=runtimeClasspath\n")
        File(repo, "buildscript-gradle.lockfile").writeText("com.example:plugin:1.0.0=classpath\n")
        runGit(repo, "add", "gradle.lockfile", "buildscript-gradle.lockfile")
        runGit(repo, "commit", "-m", "initial")
        val baseSha = runGit(repo, "rev-parse", "HEAD").trim()

        val result =
            LockfileDiffer.readFromBase(
                baseRef = baseSha,
                mainLockfile = File(repo, "gradle.lockfile"),
                buildscriptLockfile = File(repo, "buildscript-gradle.lockfile"),
                workingDir = repo,
            )

        assertEquals(
            setOf(
                Gav("com.example", "lib", "1.0.0"),
                Gav("com.example", "plugin", "1.0.0"),
            ),
            (result as LockfileDiffer.ReadFromBaseResult.Success).gavs.toSet(),
        )
    }

    @Test
    fun `readFromBase resolves lockfile path relative to git repo root`(
        @TempDir tempDir: Path,
    ) {
        val repo = tempDir.toFile()
        val subdir = File(repo, "backend").apply { mkdirs() }
        runGit(repo, "init")
        runGit(repo, "config", "user.email", "test@example.com")
        runGit(repo, "config", "user.name", "Test")
        File(repo, "backend/gradle.lockfile").writeText("com.example:lib:1.0.0=runtimeClasspath\n")
        runGit(repo, "add", "backend/gradle.lockfile")
        runGit(repo, "commit", "-m", "initial")
        val baseSha = runGit(repo, "rev-parse", "HEAD").trim()

        val result =
            LockfileDiffer.readFromBase(
                baseRef = baseSha,
                mainLockfile = File(subdir, "gradle.lockfile"),
                buildscriptLockfile = File(subdir, "buildscript-gradle.lockfile"),
                workingDir = subdir,
            )

        assertEquals(
            listOf(Gav("com.example", "lib", "1.0.0")),
            (result as LockfileDiffer.ReadFromBaseResult.Success).gavs,
        )
    }

    private fun runGit(
        repo: File,
        vararg args: String,
    ): String {
        val process =
            ProcessBuilder("git", *args)
                .directory(repo)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        check(exit == 0) { "git ${args.joinToString(" ")} failed in $repo: $output" }
        return output
    }
}
