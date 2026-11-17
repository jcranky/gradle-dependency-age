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
    val unverifiable: List<CandidateUpdate> = emptyList(),
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
        val unverifiable = mutableListOf<CandidateUpdate>()

        for (candidate in candidates) {
            val publishTime = publishTimeProvider.getPublishTime(candidate.gavForCandidate())
            when {
                publishTime != null && publishTime.isAfter(cutoff) -> rejected.add(RejectedCandidate(candidate, publishTime))
                publishTime != null -> accepted.add(candidate)
                else -> unverifiable.add(candidate)
            }
        }

        return FilterResult(accepted = accepted, rejected = rejected, unverifiable = unverifiable)
    }
}
