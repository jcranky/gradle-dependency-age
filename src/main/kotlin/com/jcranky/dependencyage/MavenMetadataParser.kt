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
        val wrapped = Regex(
            """<version>\s*<value>([^<]+)</value>\s*<lastUpdated>([^<]+)</lastUpdated>\s*</version>""",
        )
        val wrappedMatches = wrapped.findAll(xml)
        val wrappedResult = mutableMapOf<String, Instant>()
        wrappedMatches.forEach { match ->
            val version = match.groupValues[1].trim()
            val timestamp = match.groupValues[2].trim()
            wrappedResult[version] = parseTimestamp(timestamp) ?: return@forEach
        }
        if (wrappedResult.isNotEmpty()) return wrappedResult

        val versionRegex = Regex("""<version>([^<]+)</version>""")
        val timestampRegex = Regex("""<lastUpdated>([^<]+)</lastUpdated>""")
        val versions = versionRegex.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val timestamps = timestampRegex.findAll(xml).map { it.groupValues[1].trim() }.toList()
        val flatResult = mutableMapOf<String, Instant>()
        for (i in versions.indices) {
            val version = versions[i]
            val raw = timestamps.getOrNull(i) ?: continue
            val parsed = parseTimestamp(raw) ?: continue
            flatResult[version] = parsed
        }
        return flatResult
    }

    private companion object {
        private val MAVEN_TIMESTAMP_PATTERN = """^\d{14}$""".toRegex()
        private val MAVEN_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
