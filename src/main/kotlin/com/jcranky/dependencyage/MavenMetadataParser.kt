package com.jcranky.dependencyage

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MavenMetadataParser {
    fun parse(xml: String): MavenMetadata {
        val latest = extractFirst(xml, "latest")
        val lastUpdated = extractTimestamp(xml, "lastUpdated")
        val versionedLastUpdated = extractVersionedTimestamps(xml)
        return MavenMetadata(
            latest = latest,
            lastUpdated = lastUpdated,
            versionedLastUpdated = versionedLastUpdated,
        )
    }

    private fun extractFirst(
        xml: String,
        tag: String,
    ): String? {
        val regex = Regex("""<$tag>([^<]+)</$tag>""")
        return regex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun extractTimestamp(
        xml: String,
        tag: String,
    ): Instant? {
        val value = extractFirst(xml, tag) ?: return null
        return parseTimestamp(value)
    }

    private fun parseTimestamp(value: String): Instant? {
        val normalized = value.trim()
        if (normalized.matches(MAVEN_TIMESTAMP_PATTERN)) {
            return runCatching {
                LocalDateTime.parse(normalized, MAVEN_TIMESTAMP_FORMATTER)
                    .toInstant(ZoneOffset.UTC)
            }.getOrNull()
        }
        return runCatching { Instant.parse(normalized) }.getOrNull()
    }

    private fun extractVersionedTimestamps(xml: String): Map<String, Instant> {
        val result = mutableMapOf<String, Instant>()
        WRAPPED_VERSION_REGEX.findAll(xml).forEach { match ->
            val version = match.groupValues[1].trim()
            val timestamp = parseTimestamp(match.groupValues[2].trim()) ?: return@forEach
            result[version] = timestamp
        }
        // Pair a version with a lastUpdated only when the timestamp tag directly
        // follows the version tag. Positional pairing across the whole document is
        // unsound: standard maven-metadata.xml lists N plain <version> tags followed
        // by a single versioning-level <lastUpdated>, which must not be attributed
        // to the first listed version.
        ADJACENT_VERSION_REGEX.findAll(xml).forEach { match ->
            val version = match.groupValues[1].trim()
            val timestamp = parseTimestamp(match.groupValues[2].trim()) ?: return@forEach
            result[version] = timestamp
        }
        return result
    }

    private companion object {
        private val MAVEN_TIMESTAMP_PATTERN = """^\d{14}$""".toRegex()
        private val MAVEN_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        private val WRAPPED_VERSION_REGEX = Regex(
            """<version>\s*<value>([^<]+)</value>\s*<lastUpdated>([^<]+)</lastUpdated>\s*</version>""",
        )
        private val ADJACENT_VERSION_REGEX = Regex(
            """<version>([^<]+)</version>\s*<lastUpdated>([^<]+)</lastUpdated>""",
        )
    }
}
