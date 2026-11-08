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
    val unknownPublishTimes: List<Gav> = emptyList(),
) {
    val isOk: Boolean get() = violations.isEmpty() && unknownPublishTimes.isEmpty()
}

class DependencyAgeVerifier(
    private val clock: Clock,
) {
    fun verifyFromLookup(
        publishTimes: Map<Gav, Instant?>,
        minAgeDays: Int,
    ): VerificationResult {
        val cutoff = Instant.now(clock).minus(minAgeDays.toLong(), ChronoUnit.DAYS)
        val violations =
            publishTimes
                .mapNotNull { (gav, publishTime) -> publishTime?.let { gav to it } }
                .filter { (_, publishTime) -> publishTime.isAfter(cutoff) }
                .map { (gav, publishTime) -> Violation(gav, publishTime) }
        val unknownPublishTimes = publishTimes.filterValues { it == null }.keys.toList()
        return VerificationResult(
            violations = violations,
            totalChecked = publishTimes.size,
            unknownPublishTimes = unknownPublishTimes,
        )
    }
}
