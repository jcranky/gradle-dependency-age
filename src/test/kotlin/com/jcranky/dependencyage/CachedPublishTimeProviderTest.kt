package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class CachedPublishTimeProviderTest {
    @Test
    fun `returns cached value when cache is fresh`() {
        val cacheFile = File.createTempFile("dep-age-cache", ".json").also { it.deleteOnExit() }
        val gav = Gav("com.example", "lib", "1.0.0")
        val fixedNow = Instant.parse("2026-09-11T00:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val publishTime = Instant.parse("2025-01-01T00:00:00Z")

        cacheFile.writeText(
            """
            {
              "${gav}": {
                "publishTime": "$publishTime",
                "fetchedAt": "$fixedNow"
              }
            }
            """.trimIndent(),
        )

        val delegate = RecordingProvider(mapOf(gav to publishTime))
        val provider = CachedPublishTimeProvider(delegate, cacheFile, Duration.ofDays(30), clock)

        val result = provider.getPublishTime(gav)

        assertEquals(publishTime, result)
        assertEquals(0, delegate.callCount, "delegate should not be called when cache is fresh")
    }

    @Test
    fun `fetches from delegate when cache is missing`() {
        val cacheFile = File.createTempFile("dep-age-cache", ".json").also { it.deleteOnExit() }
        cacheFile.writeText("{}")
        val gav = Gav("com.example", "lib", "1.0.0")
        val publishTime = Instant.parse("2025-01-01T00:00:00Z")

        val delegate = RecordingProvider(mapOf(gav to publishTime))
        val provider = CachedPublishTimeProvider(delegate, cacheFile, Duration.ofDays(30))

        val result = provider.getPublishTime(gav)

        assertEquals(publishTime, result)
        assertEquals(1, delegate.callCount)
        assertTrue(cacheFile.readText().contains(publishTime.toString()))
    }

    @Test
    fun `fetches from delegate when cache is stale`() {
        val cacheFile = File.createTempFile("dep-age-cache", ".json").also { it.deleteOnExit() }
        val gav = Gav("com.example", "lib", "1.0.0")
        val fixedNow = Instant.parse("2026-09-11T00:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val staleFetchedAt = fixedNow.minus(Duration.ofDays(60))
        val newPublishTime = Instant.parse("2025-06-01T00:00:00Z")

        cacheFile.writeText(
            """
            {
              "${gav}": {
                "publishTime": "2025-01-01T00:00:00Z",
                "fetchedAt": "$staleFetchedAt"
              }
            }
            """.trimIndent(),
        )

        val delegate = RecordingProvider(mapOf(gav to newPublishTime))
        val provider = CachedPublishTimeProvider(delegate, cacheFile, Duration.ofDays(30), clock)

        val result = provider.getPublishTime(gav)

        assertEquals(newPublishTime, result)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `returns null and does not cache when delegate returns null`() {
        val cacheFile = File.createTempFile("dep-age-cache", ".json").also { it.deleteOnExit() }
        cacheFile.writeText("{}")
        val gav = Gav("com.example", "lib", "1.0.0")

        val delegate = RecordingProvider(emptyMap())
        val provider = CachedPublishTimeProvider(delegate, cacheFile, Duration.ofDays(30))

        val result = provider.getPublishTime(gav)

        assertNull(result)
        assertEquals("{}", cacheFile.readText().trim())
    }

    @Test
    fun `handles missing cache file by creating one`() {
        val cacheFile = File.createTempFile("dep-age-cache", ".json").also { it.deleteOnExit() }
        cacheFile.delete()
        val gav = Gav("com.example", "lib", "1.0.0")
        val publishTime = Instant.parse("2025-01-01T00:00:00Z")

        val delegate = RecordingProvider(mapOf(gav to publishTime))
        val provider = CachedPublishTimeProvider(delegate, cacheFile, Duration.ofDays(30))

        val result = provider.getPublishTime(gav)

        assertEquals(publishTime, result)
        assertTrue(cacheFile.exists())
    }
}

private class RecordingProvider(
    private val responses: Map<Gav, Instant>,
) : PublishTimeProvider {
    var callCount = 0
        private set

    override fun getPublishTime(gav: Gav): Instant? {
        callCount++
        return responses[gav]
    }
}
