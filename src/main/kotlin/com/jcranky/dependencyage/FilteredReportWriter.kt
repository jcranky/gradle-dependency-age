package com.jcranky.dependencyage

import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

class FilteredReportWriter(
    private val clock: Clock,
) {
    fun write(
        outputFile: File,
        accepted: List<CandidateUpdate>,
        rejected: List<RejectedCandidate>,
        minAgeDays: Int,
        unverified: List<CandidateUpdate> = emptyList(),
    ) {
        outputFile.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.append("# Filtered dependencyUpdates report\n")
        sb.append("# Min age: $minAgeDays days\n")
        sb.append("# Generated: ${Instant.now(clock)}\n")
        sb.append("# Accepted: ${accepted.size}, Rejected: ${rejected.size}, Unverifiable: ${unverified.size}\n")
        sb.append("\n")
        sb.append("## Accepted candidates (older than $minAgeDays days)\n")
        accepted.forEach { candidate ->
            sb.append(" - ${candidate.group}:${candidate.artifact}:${candidate.currentVersion} -> ${candidate.newVersion}\n")
        }
        sb.append("\n")
        sb.append("## Rejected candidates (too recent)\n")
        rejected.forEach { rejected ->
            val daysAgo =
                ChronoUnit.DAYS.between(rejected.publishTime, Instant.now(clock))
            sb.append(
                " # FILTERED: ${rejected.candidate.group}:${rejected.candidate.artifact}:" +
                    "${rejected.candidate.currentVersion} -> ${rejected.candidate.newVersion}" +
                    " (published $daysAgo days ago)\n",
            )
        }
        if (unverified.isNotEmpty()) {
            sb.append("\n")
            sb.append("## Unverifiable candidates (publication date unknown)\n")
            unverified.forEach { candidate ->
                sb.append(
                    " # UNVERIFIED: ${candidate.group}:${candidate.artifact}:" +
                        "${candidate.currentVersion} -> ${candidate.newVersion}" +
                        " (publication date unknown)\n",
                )
            }
        }
        outputFile.writeText(sb.toString())
    }
}
