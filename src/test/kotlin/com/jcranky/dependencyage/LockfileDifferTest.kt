package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LockfileDifferTest {
    @Test
    fun `diff returns GAVs added in current that were not in previous`() {
        val previous = listOf(Gav("com.example", "lib", "1.0.0"))
        val current = listOf(Gav("com.example", "lib", "1.0.0"), Gav("com.example", "new", "1.0.0"))

        val result = LockfileDiffer.diff(current, previous)

        assertEquals(listOf(Gav("com.example", "new", "1.0.0")), result)
    }

    @Test
    fun `diff returns GAVs removed from current that were in previous`() {
        val previous = listOf(Gav("com.example", "lib", "1.0.0"), Gav("com.example", "old", "1.0.0"))
        val current = listOf(Gav("com.example", "lib", "1.0.0"))

        val result = LockfileDiffer.diff(current, previous)

        assertEquals(listOf(Gav("com.example", "old", "1.0.0")), result)
    }

    @Test
    fun `diff detects version bumps as both old and new version`() {
        val previous = listOf(Gav("com.example", "lib", "1.0.0"))
        val current = listOf(Gav("com.example", "lib", "2.0.0"))

        val result = LockfileDiffer.diff(current, previous)

        assertEquals(
            setOf(
                Gav("com.example", "lib", "1.0.0"),
                Gav("com.example", "lib", "2.0.0"),
            ),
            result.toSet(),
        )
    }

    @Test
    fun `diff returns empty list when current and previous are identical`() {
        val gavs = listOf(Gav("com.example", "lib", "1.0.0"), Gav("com.example", "other", "2.0.0"))

        val result = LockfileDiffer.diff(gavs, gavs)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `diff returns all of current when previous is empty`() {
        val current = listOf(Gav("com.example", "lib", "1.0.0"), Gav("com.example", "other", "2.0.0"))

        val result = LockfileDiffer.diff(current, emptyList())

        assertEquals(current.toSet(), result.toSet())
    }

    @Test
    fun `diff deduplicates results`() {
        val previous = listOf(Gav("com.example", "lib", "1.0.0"), Gav("com.example", "lib", "1.0.0"))
        val current = listOf(Gav("com.example", "lib", "2.0.0"), Gav("com.example", "lib", "2.0.0"))

        val result = LockfileDiffer.diff(current, previous)

        assertEquals(
            setOf(
                Gav("com.example", "lib", "1.0.0"),
                Gav("com.example", "lib", "2.0.0"),
            ),
            result.toSet(),
        )
    }
}
