package com.jcranky.dependencyage

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Dogfooding: runs this repo's own plugins against this repo's own real
 * lockfiles (fixtures copied from the committed `gradle.lockfile` /
 * `buildscript-gradle.lockfile`), through TestKit, inside a synthetic git
 * repo so the differ can read lockfiles at a base ref.
 */
class DependencyAgeSelfVerificationTest {

    @field:TempDir
    lateinit var projectDir: File

    private val seededGav = "com.example.selfcheck:seeded-too-young:1.0.0"

    @Test
    fun selfCheckAcceptsTheReposOwnUnchangedLockfiles() {
        setupProject()
        copyRepoLockfiles()
        val baseRef = commitCurrentState()

        runVerify(baseRef, shouldFail = false).let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":verifyDependencyAge")!!.outcome)
            assertTrue("No dependency changes" in result.output, "unexpected output:\n${result.output}")
        }
    }

    @Test
    fun selfCheckFailsWhenASeededTooYoungDependencyIsAdded() {
        setupProject()
        copyRepoLockfiles()
        val baseRef = commitCurrentState()
        File(projectDir, "gradle.lockfile").appendText("$seededGav=compileClasspath\n")
        seedCache()

        val result = runVerify(baseRef, shouldFail = true)

        assertEquals(TaskOutcome.FAILED, result.task(":verifyDependencyAge")!!.outcome)
        assertTrue(seededGav in result.output, "violation message should contain $seededGav:\n${result.output}")
    }

    /**
     * The publish-time provider is not injectable through TestKit, but its
     * cache is: pre-seed an entry with `publishTime = fetchedAt = now` for the
     * seeded GAV so the violation path is deterministic and network-free.
     */
    private fun seedCache() {
        val now = java.time.Instant.now()
        File(projectDir, "build/dependency-age-cache.json").apply { parentFile.mkdirs() }
            .writeText(
                """
                {
                  "$seededGav": {
                    "publishTime": "$now",
                    "fetchedAt": "$now"
                  }
                }
                """.trimIndent() + "\n",
            )
    }

    private fun setupProject() {
        File(projectDir, "settings.gradle.kts")
            .writeText("rootProject.name = \"dependency-age-selfcheck\"\n")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.jcranky.dependency-age-verify")
            }
            """.trimIndent() + "\n",
        )
    }

    private fun copyRepoLockfiles() {
        val (main, buildscript) = repoLockfiles()
        main.copyTo(File(projectDir, "gradle.lockfile"), overwrite = true)
        buildscript.copyTo(File(projectDir, "buildscript-gradle.lockfile"), overwrite = true)
    }

    private fun commitCurrentState(): String {
        git("init", "-q")
        git("add", "-A")
        git("-c", "user.email=selfcheck@jcranky", "-c", "user.name=selfcheck", "commit", "-qm", "base")
        return git("rev-parse", "HEAD").trim()
    }

    private fun runVerify(baseRef: String, shouldFail: Boolean) =
        GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
            .withArguments("verifyDependencyAge", "-PbaseRef=$baseRef")
            .let { runner -> if (shouldFail) runner.buildAndFail() else runner.build() }

    private fun repoLockfiles(): Pair<File, File> {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .firstOrNull { it.resolve("gradle.lockfile").exists() && it.resolve("buildscript-gradle.lockfile").exists() }
            ?: error("could not locate the plugin repo root (gradle.lockfile) from ${System.getProperty("user.dir")}")
        return root.resolve("gradle.lockfile") to root.resolve("buildscript-gradle.lockfile")
    }

    private fun git(vararg args: String): String {
        val process = ProcessBuilder("git", *args).directory(projectDir).start()
        val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) error("git ${args.joinToString(" ")} failed (exit $exit): $output")
        return output
    }
}
