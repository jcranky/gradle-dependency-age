package com.jcranky.dependencyage

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * The verify and filter tasks share `<buildDir>/dependency-age-cache.json` by design
 * (a filter run warms the verify gate and vice versa). Both must be able to run in
 * a single build invocation without Gradle's overlapping-output/implicit-dependency
 * validation failing one of them.
 */
class DependencyAgeSharedCacheTest {

    @field:TempDir
    lateinit var projectDir: File

    // Real, published artifacts whose `latest` matches the queried version, so the
    // maven-metadata lookup resolves without depending on the solrsearch index.
    private val verifyChangedGav = "at.favre.lib:bcrypt:0.10.2"
    private val filterCandidate = " - ch.qos.logback:logback-classic:1.5.0 -> 1.6.7"

    @Test
    fun verifyAndFilterTasksCanRunInTheSameBuild() {
        setupProject()
        createLockfiles()
        val baseRef = commitCurrentState()
        File(projectDir, "gradle.lockfile").appendText("$verifyChangedGav=compileClasspath\n")
        File(projectDir, "build/dependencyUpdates/report.txt").apply {
            parentFile.mkdirs()
            writeText("The following dependencies have later milestone versions:\n$filterCandidate\n")
        }

        val result =
            GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .withArguments("verifyDependencyAge", "filterCandidatesByAge", "-PbaseRef=$baseRef")
                .build()

        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":verifyDependencyAge")!!.outcome,
            "verify task must succeed in a combined run:\n${result.output}",
        )
        assertEquals(
            TaskOutcome.SUCCESS,
            result.task(":filterCandidatesByAge")!!.outcome,
            "filter task must succeed in a combined run:\n${result.output}",
        )
    }

    private fun setupProject() {
        File(projectDir, "settings.gradle.kts")
            .writeText("rootProject.name = \"dependency-age-shared-cache\"\n")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.jcranky.dependency-age-verify")
                id("com.jcranky.dependency-age-filter")
            }
            """.trimIndent() + "\n",
        )
    }

    private fun createLockfiles() {
        File(projectDir, "gradle.lockfile").writeText("ch.qos.logback:logback-classic:1.6.3=compileClasspath\n")
        File(projectDir, "buildscript-gradle.lockfile").writeText("")
    }

    private fun commitCurrentState(): String {
        git("init", "-q")
        git("add", "-A")
        git("-c", "user.email=sharedcache@jcranky", "-c", "user.name=sharedcache", "commit", "-qm", "base")
        return git("rev-parse", "HEAD").trim()
    }

    private fun git(vararg args: String): String {
        val process = ProcessBuilder("git", *args).directory(projectDir).start()
        val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) error("git ${args.joinToString(" ")} failed (exit $exit): $output")
        return output
    }
}
