package com.jcranky.dependencyage

import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant

class CachedPublishTimeProvider(
    private val delegate: PublishTimeProvider,
    private val cacheFile: File,
    private val cacheTtl: Duration,
    private val clock: Clock = Clock.systemUTC(),
) : PublishTimeProvider {
    override fun getPublishTime(gav: Gav): Instant? {
        val cache = readCache()
        val cached = cache[gav.toString()]
        if (cached != null && Duration.between(cached.fetchedAt, Instant.now(clock)) < cacheTtl) {
            return cached.publishTime
        }

        val publishTime = delegate.getPublishTime(gav) ?: return null
        cache[gav.toString()] = CacheEntry(publishTime, Instant.now(clock))
        writeCache(cache)
        return publishTime
    }

    private fun readCache(): MutableMap<String, CacheEntry> {
        if (!cacheFile.exists()) return mutableMapOf()
        val text = cacheFile.readText()
        if (text.isBlank()) return mutableMapOf()
        val map = mutableMapOf<String, CacheEntry>()
        Regex("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"publishTime\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"fetchedAt\"\\s*:\\s*\"([^\"]+)\"\\s*}")
            .findAll(text)
            .forEach { match ->
                val key = match.groupValues[1]
                val publishTime = Instant.parse(match.groupValues[2])
                val fetchedAt = Instant.parse(match.groupValues[3])
                map[key] = CacheEntry(publishTime, fetchedAt)
            }
        return map
    }

    private fun writeCache(entries: Map<String, CacheEntry>) {
        val sb = StringBuilder("{\n")
        entries.entries.forEachIndexed { index, (key, entry) ->
            if (index > 0) sb.append(",\n")
            sb.append("  \"").append(key).append("\": {\n")
            sb.append("    \"publishTime\": \"").append(entry.publishTime).append("\",\n")
            sb.append("    \"fetchedAt\": \"").append(entry.fetchedAt).append("\"\n")
            sb.append("  }")
        }
        sb.append("\n}")
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(sb.toString())
    }

    private data class CacheEntry(
        val publishTime: Instant,
        val fetchedAt: Instant,
    )
}
