package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DependencyAgeTasksTest {

    private val gav = Gav("com.example", "unknown-age", "1.0")

    @Test
    fun lookupPublishTimesKeepsDependenciesWithUnknownPublicationTime() {
        val lookup = lookupPublishTimes(FakePublishTimeProvider(), listOf(gav))

        assertTrue(gav in lookup.keys, "unknown publication time must stay in the lookup so the verifier can fail closed; got $lookup")
        assertEquals(null, lookup[gav])
    }

    @Test
    fun lookupPublishTimesKeepsDependenciesWhoseLookupFailed() {
        val lookup = lookupPublishTimes(
            FakePublishTimeProvider(failures = setOf(gav)),
            listOf(gav),
        )

        assertTrue(gav in lookup.keys, "failed lookups must stay in the lookup so the verifier can fail closed; got $lookup")
        assertEquals(null, lookup[gav])
    }
}
