package com.jcranky.dependencyage

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

data class RejectedCandidate(
    val candidate: CandidateUpdate,
    val publishTime: Instant,
)

data class FilterResult(
    val accepted: List<CandidateUpdate>,
    val rejected: List<RejectedCandidate>,
) {
    val isOk: Boolean get() = rejected.isEmpty()
}

class CandidateFilter(
    private val publishTimeProvider: PublishTimeProvider,
    private val clock: Clock,
) {
    fun filter(
        candidates: List<CandidateUpdate>,
        minAgeDays: Int,
    ): FilterResult {
        val cutoff = Instant.now(clock).minus(minAgeDays.toLong(), ChronoUnit.DAYS)
        val accepted = mutableListOf<CandidateUpdate>()
        val rejected = mutableListOf<RejectedCandidate>()

        for (candidate in candidates) {
            val publishTime = publishTimeProvider.getPublishTime(candidate.gavForCandidate())
            if (publishTime != null && publishTime.isAfter(cutoff)) {
                rejected.add(RejectedCandidate(candidate, publishTime))
            } else {
                accepted.add(candidate)
            }
        }

        return FilterResult(accepted = accepted, rejected = rejected)
    }
}
