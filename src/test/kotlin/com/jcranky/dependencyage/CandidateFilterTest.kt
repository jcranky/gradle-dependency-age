package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CandidateFilterTest {
    @Test
    fun `accepts all candidates when all are older than cutoff`() {
        val candidates =
            listOf(
                CandidateUpdate("com.example", "lib-a", "1.0.0", "2.0.0"),
                CandidateUpdate("com.example", "lib-b", "1.0.0", "2.0.0"),
            )
        val provider =
            FakePublishTimeProvider(
                publishTimes =
                    mapOf(
                        Gav("com.example", "lib-a", "2.0.0") to Instant.parse("2020-01-01T00:00:00Z"),
                        Gav("com.example", "lib-b", "2.0.0") to Instant.parse("2020-06-01T00:00:00Z"),
                    ),
            )
        val filter = CandidateFilter(provider, fixedClock(Instant.parse("2026-09-11T00:00:00Z")))

        val result = filter.filter(candidates, minAgeDays = 7)

        assertTrue(result.isOk)
        assertEquals(2, result.accepted.size)
        assertEquals(emptyList<RejectedCandidate>(), result.rejected)
    }

    @Test
    fun `rejects candidates whose new version is younger than cutoff`() {
        val candidates =
            listOf(
                CandidateUpdate("com.example", "lib-old", "1.0.0", "2.0.0"),
                CandidateUpdate("com.example", "lib-new", "1.0.0", "2.0.0"),
            )
        val recent = Instant.parse("2026-09-09T00:00:00Z")
        val provider =
            FakePublishTimeProvider(
                publishTimes =
                    mapOf(
                        Gav("com.example", "lib-old", "2.0.0") to Instant.parse("2020-01-01T00:00:00Z"),
                        Gav("com.example", "lib-new", "2.0.0") to recent,
                    ),
            )
        val filter = CandidateFilter(provider, fixedClock(Instant.parse("2026-09-11T00:00:00Z")))

        val result = filter.filter(candidates, minAgeDays = 7)

        assertEquals(false, result.isOk)
        assertEquals(1, result.accepted.size)
        assertEquals("com.example:lib-old", "${result.accepted.single().group}:${result.accepted.single().artifact}")
        assertEquals(1, result.rejected.size)
        assertEquals("com.example:lib-new", "${result.rejected.single().candidate.group}:${result.rejected.single().candidate.artifact}")
        assertEquals(recent, result.rejected.single().publishTime)
    }

    @Test
    fun `excludes candidates with unknown publish time from the curated list`() {
        val candidates = listOf(CandidateUpdate("com.example", "lib", "1.0.0", "2.0.0"))
        val provider = FakePublishTimeProvider(publishTimes = emptyMap())
        val filter = CandidateFilter(provider, fixedClock(Instant.parse("2026-09-11T00:00:00Z")))

        val result = filter.filter(candidates, minAgeDays = 7)

        assertTrue(result.isOk)
        assertEquals(0, result.accepted.size, "an unverifiable candidate must not be recommended as safe")
        assertEquals(1, result.unverifiable.size)
        assertEquals("com.example:lib", "${result.unverifiable.single().group}:${result.unverifiable.single().artifact}")
    }

    private fun fixedClock(instant: Instant): Clock = Clock.fixed(instant, ZoneOffset.UTC)
}
