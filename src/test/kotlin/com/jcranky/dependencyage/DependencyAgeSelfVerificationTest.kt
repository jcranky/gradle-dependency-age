package com.jcranky.dependencyage

import java.io.File
import java.time.Instant
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

    private fun seedCache() {
        SelfVerificationSupport.seedPublishTimeCache(projectDir, mapOf(seededGav to Instant.now()))
    }

    @Test
    fun selfCheckFailsWhenAPublicationTimeCannotBeDetermined() {
        setupProject()
        copyRepoLockfiles()
        val baseRef = commitCurrentState()
        File(projectDir, "gradle.lockfile")
            .appendText("com.example.selfcheck:unresolvable-publication-time:1.0.0=compileClasspath\n")

        val result = runVerify(baseRef, shouldFail = true)

        assertEquals(
            TaskOutcome.FAILED,
            result.task(":verifyDependencyAge")!!.outcome,
            "verify must fail closed when a changed dependency's publication date is unknown:\n${result.output}",
        )
        assertTrue(
            "unresolvable-publication-time" in result.output,
            "failure message should name the dependency with the unknown publish time:\n${result.output}",
        )
    }

    @Test
    fun selfCheckRunsUnderConfigurationCache() {
        setupProject()
        copyRepoLockfiles()
        val baseRef = commitCurrentState()

        val result =
            GradleRunner.create().withPluginClasspath().withProjectDir(projectDir)
                .withArguments("verifyDependencyAge", "-PbaseRef=$baseRef", "--configuration-cache")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":verifyDependencyAge")!!.outcome,
            "verify must succeed under the configuration cache:\n${result.output}",
        )
        assertTrue(
            "Configuration cache problems" !in result.output && "configuration cache" !in result.output.replace("Consider enabling configuration cache", ""),
            "verify must not produce configuration cache problems:\n${result.output}",
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

    private fun repoLockfiles(): Pair<File, File> = SelfVerificationSupport.repoLockfiles()

    private fun git(vararg args: String): String {
        val process = ProcessBuilder("git", *args).directory(projectDir).start()
        val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) error("git ${args.joinToString(" ")} failed (exit $exit): $output")
        return output
    }
}
