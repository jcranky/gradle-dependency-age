package com.jcranky.dependencyage

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

data class Violation(
    val gav: Gav,
    val publishTime: Instant,
)

data class VerificationResult(
    val violations: List<Violation>,
    val totalChecked: Int,
) {
    val isOk: Boolean get() = violations.isEmpty()
}

class DependencyAgeVerifier(
    private val clock: Clock,
) {
    fun verifyFromLookup(
        publishTimes: Map<Gav, Instant>,
        minAgeDays: Int,
    ): VerificationResult {
        val cutoff = Instant.now(clock).minus(minAgeDays.toLong(), ChronoUnit.DAYS)
        val violations =
            publishTimes
                .filter { (_, publishTime) -> publishTime.isAfter(cutoff) }
                .map { (gav, publishTime) -> Violation(gav, publishTime) }
        return VerificationResult(violations = violations, totalChecked = publishTimes.size)
    }
}
