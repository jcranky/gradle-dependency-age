package com.jcranky.dependencyage

import java.io.File
import java.time.Instant

/**
 * Shared helpers for the self-verification (dogfooding) tests: locating this
 * repo's committed lockfiles and pre-seeding the shared publish-time cache
 * so TestKit runs stay deterministic and network-free.
 */
object SelfVerificationSupport {

    /**
     * Anchors the plugin repo root at the nearest `.git` ancestor of the test
     * worker's working directory (the lockfile-sniffing alternative could
     * bind to a nested repo that also happens to carry lockfiles).
     */
    fun repoLockfiles(): Pair<File, File> {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .firstOrNull { it.resolve(".git").exists() }
            ?: error("could not locate the plugin repo root (.git) from ${System.getProperty("user.dir")}")
        val main = root.resolve("gradle.lockfile")
        val buildscript = root.resolve("buildscript-gradle.lockfile")
        require(main.exists() && buildscript.exists()) {
            "lockfiles missing at the detected repo root $root — run ./gradlew :dependencies --write-locks"
        }
        return main to buildscript
    }

    fun seedPublishTimeCache(projectDir: File, entries: Map<String, Instant>) {
        val cacheFile = File(projectDir, "build/dependency-age-cache.json")
        cacheFile.parentFile.mkdirs()
        val jsonEntries = entries.entries.joinToString(",\n") { (gav, publishTime) ->
            """
            "$gav": {
              "publishTime": "$publishTime",
              "fetchedAt": "${Instant.now()}"
            }
            """.trimIndent()
        }
        cacheFile.writeText("{\n$jsonEntries\n}\n")
    }
}
