package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DependencyAgeVerifierTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)
    private val verifier = DependencyAgeVerifier(clock)

    @Test
    fun `verifyFromLookup passes when all publish times are older than cutoff`() {
        val result =
            verifier.verifyFromLookup(
                publishTimes =
                    mapOf(
                        Gav("com.example", "lib-a", "1.0.0") to Instant.parse("2020-01-01T00:00:00Z"),
                        Gav("com.example", "lib-b", "2.0.0") to Instant.parse("2020-06-01T00:00:00Z"),
                    ),
                minAgeDays = 7,
            )

        assertTrue(result.isOk)
        assertEquals(0, result.violations.size)
        assertEquals(2, result.totalChecked)
    }

    @Test
    fun `verifyFromLookup reports violation when a publish time is younger than cutoff`() {
        val recent = Instant.parse("2026-09-09T00:00:00Z")
        val result =
            verifier.verifyFromLookup(
                publishTimes =
                    mapOf(
                        Gav("com.example", "lib-old", "1.0.0") to Instant.parse("2020-01-01T00:00:00Z"),
                        Gav("com.example", "lib-new", "2.0.0") to recent,
                    ),
                minAgeDays = 7,
            )

        assertEquals(false, result.isOk)
        assertEquals(1, result.violations.size)
        assertEquals(Gav("com.example", "lib-new", "2.0.0"), result.violations.single().gav)
        assertEquals(recent, result.violations.single().publishTime)
    }

    @Test
    fun `verifyFromLookup ignores gavs not present in the lookup map`() {
        val result =
            verifier.verifyFromLookup(
                publishTimes =
                    mapOf(
                        Gav("com.example", "lib-a", "1.0.0") to Instant.parse("2020-01-01T00:00:00Z"),
                    ),
                minAgeDays = 7,
            )

        assertEquals(1, result.totalChecked)
    }
}
