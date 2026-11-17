package com.jcranky.dependencyage

import java.io.File

data class CandidateUpdate(
    val group: String,
    val artifact: String,
    val currentVersion: String,
    val newVersion: String,
) {
    fun gavForCandidate(): Gav = Gav(group, artifact, newVersion)
}

object DependencyUpdatesReportParser {
    private val VERSION_CHAR = """[\w.\-+${'$'}]"""
    private val UNBRACKETED_LINE =
        Regex(
            """^\s*-\s*([\w.\-]+):([\w.\-]+):($VERSION_CHAR+)\s*->\s*($VERSION_CHAR+)\s*$""",
        )
    private val BRACKETED_LINE =
        Regex(
            """^\s*-\s*([\w.\-]+):([\w.\-]+)\s\[($VERSION_CHAR+)((?:\s*->\s*$VERSION_CHAR+)*)\]\s*$""",
        )

    fun parse(file: File): List<CandidateUpdate> {
        if (!file.exists()) return emptyList()
        val candidates = mutableListOf<CandidateUpdate>()
        file.useLines { lines ->
            lines.forEach { line ->
                parseLine(line)?.let { candidates.add(it) }
            }
        }
        return candidates
    }

    /**
     * Matches both report formats emitted by the ben-manes versions plugin:
     * - `- g:a:cur -> new` (older plugin versions)
     * - `- g:a [cur -> intermediate -> latest]` (current plugin versions,
     *   where the last version in the chain is the newest milestone)
     */
    private fun parseLine(line: String): CandidateUpdate? {
        UNBRACKETED_LINE.matchEntire(line)?.let { match ->
            return CandidateUpdate(
                group = match.groupValues[1],
                artifact = match.groupValues[2],
                currentVersion = match.groupValues[3],
                newVersion = match.groupValues[4],
            )
        }
        val match = BRACKETED_LINE.matchEntire(line) ?: return null
        val versions =
            (match.groupValues[3] + match.groupValues[4])
                .split("->")
                .map { it.trim() }
        return CandidateUpdate(
            group = match.groupValues[1],
            artifact = match.groupValues[2],
            currentVersion = versions.first(),
            newVersion = versions.last(),
        )
    }
}
