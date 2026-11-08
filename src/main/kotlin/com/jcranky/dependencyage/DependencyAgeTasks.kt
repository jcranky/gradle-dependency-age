package com.jcranky.dependencyage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val sharedLogger = Logging.getLogger("DependencyAgeTasks")

abstract class VerifyDependencyAgeTask : DefaultTask() {
    @get:Input
    abstract val minAgeDays: Property<Int>

    @get:Input
    abstract val baseRef: Property<String>

    @get:InputFile
    abstract val mainLockfile: RegularFileProperty

    @get:InputFile
    abstract val buildscriptLockfile: RegularFileProperty

    @get:OutputFile
    abstract val cacheFile: RegularFileProperty

    init {
        group = "verification"
        description = "Fails if any dependency changed in this branch was published within minAgeDays"
    }

    @TaskAction
    fun verify() {
        val ref = baseRef.get()
        if (ref.isBlank()) {
            throw GradleException(
                "verifyDependencyAge: baseRef is blank. " +
                    "Set -PbaseRef=<ref>, or run in an MR/PR pipeline that provides " +
                    "the diff base (e.g. CI_MERGE_REQUEST_DIFF_BASE_SHA in GitLab MR pipelines) " +
                    "and map it via -PbaseRef. Refusing to scan the full " +
                    "lockfile to avoid silently bypassing diff-based verification.",
            )
        }
        val current =
            (LockfileParser.parse(mainLockfile.get().asFile) +
                LockfileParser.parse(buildscriptLockfile.get().asFile)).distinct()
        when (
            val outcome =
                LockfileDiffer.readFromBase(
                    baseRef = ref,
                    mainLockfile = mainLockfile.get().asFile,
                    buildscriptLockfile = buildscriptLockfile.get().asFile,
                    workingDir = project.rootDir,
                )
        ) {
            is LockfileDiffer.ReadFromBaseResult.Failure -> {
                throw GradleException(buildFailureMessage(ref, outcome))
            }
            is LockfileDiffer.ReadFromBaseResult.Success -> {
                val changed = LockfileDiffer.diff(current, outcome.gavs)
                if (changed.isEmpty()) {
                    logger.lifecycle("No dependency changes vs $ref; nothing to verify")
                    return
                }
                logger.lifecycle(
                    "Checking publication dates for ${changed.size} changed dependencies (vs $ref)",
                )
                val lookup = lookupPublishTimes(PublishTimeProviders.cached(cacheFile.get().asFile), changed)
                val result = DependencyAgeVerifier(Clock.systemUTC()).verifyFromLookup(lookup, minAgeDays.get())
                if (!result.isOk) {
                    throw GradleException(buildViolationMessage(result, minAgeDays.get()))
                }
                logger.lifecycle(
                    "Verified ${changed.size} changed dependencies are at least ${minAgeDays.get()} days old",
                )
            }
        }
    }

    private fun buildFailureMessage(
        ref: String,
        outcome: LockfileDiffer.ReadFromBaseResult.Failure,
    ): String =
        "verifyDependencyAge: failed to read lockfiles at base ref $ref\n" +
            "  reason: ${outcome.reason}" +
            outcome.stderr?.let { "\n  stderr (first 5 lines):\n$it" }.orEmpty()

    private fun buildViolationMessage(
        result: VerificationResult,
        minAgeDays: Int,
    ): String {
        val sb = StringBuilder()
        if (result.violations.isNotEmpty()) {
            sb.append("${result.violations.size} changed dependencies were published within the last $minAgeDays days:\n")
            result.violations.forEach { v ->
                sb.append("  - ${v.gav} (published ${v.publishTime})\n")
            }
            sb.append("\nWait for the dependency to age out, or override with -PminDepAgeDays=0 if urgent.")
        }
        if (result.unknownPublishTimes.isNotEmpty()) {
            sb.append(
                "${result.unknownPublishTimes.size} changed dependencies could not be age-verified " +
                    "because their publication date could not be determined (failing closed):\n",
            )
            result.unknownPublishTimes.forEach { gav ->
                sb.append("  - $gav (no publication date available)\n")
            }
            sb.append(
                "\nThis cannot be overridden with -PminDepAgeDays=0: without a publication date there is " +
                    "no verified age. Check that the coordinates resolve correctly.",
            )
        }
        return sb.toString().trimEnd()
    }
}

abstract class FilterCandidatesByAgeTask : DefaultTask() {
    @get:Input
    abstract val minAgeDays: Property<Int>

    @get:InputFile
    abstract val dependencyUpdatesReport: RegularFileProperty

    @get:OutputFile
    abstract val filteredReport: RegularFileProperty

    @get:OutputFile
    abstract val cacheFile: RegularFileProperty

    init {
        group = "verification"
        description = "Filters dependencyUpdates report by publication age"
    }

    @TaskAction
    fun filter() {
        val candidates = DependencyUpdatesReportParser.parse(dependencyUpdatesReport.get().asFile)
        logger.lifecycle("Filtering ${candidates.size} candidates by publication age")
        val provider = PublishTimeProviders.cached(cacheFile.get().asFile)
        val result = CandidateFilter(provider, Clock.systemUTC()).filter(candidates, minAgeDays.get())
        FilteredReportWriter(Clock.systemUTC())
            .write(
                outputFile = filteredReport.get().asFile,
                accepted = result.accepted,
                rejected = result.rejected,
                minAgeDays = minAgeDays.get(),
                unverified = result.unverifiable,
            )
        logger.lifecycle(
            "Filtered dependencyUpdates: ${result.accepted.size} accepted, ${result.rejected.size} rejected, " +
                "${result.unverifiable.size} unverifiable",
        )
    }
}

object PublishTimeProviders {
    fun cached(cacheFile: File): PublishTimeProvider {
        val metadataFetcher = DefaultMavenMetadataFetcher()
        val solrsearchFetcher = DefaultSolrsearchFetcher(DefaultHttpFetcher())
        val hybrid = HybridPublishTimeProvider(metadataFetcher, solrsearchFetcher)
        return CachedPublishTimeProvider(hybrid, cacheFile, Duration.ofDays(30))
    }
}

internal fun lookupPublishTimes(
    provider: PublishTimeProvider,
    gavs: List<Gav>,
): Map<Gav, Instant?> =
    runBlocking {
        coroutineScope {
            gavs
                .map { gav ->
                    async(Dispatchers.IO) {
                        try {
                            gav to provider.getPublishTime(gav)
                        } catch (e: Exception) {
                            sharedLogger.warn("Failed to fetch publish time for $gav: ${e.message}")
                            gav to null
                        }
                    }
                }.awaitAll()
                .associate { it.first to it.second }
        }
    }
