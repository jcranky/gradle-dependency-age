package com.jcranky.dependencyage

import java.time.Instant

class FakePublishTimeProvider(
    private val publishTimes: Map<Gav, Instant> = emptyMap(),
    private val failures: Set<Gav> = emptySet(),
) : PublishTimeProvider {
    override fun getPublishTime(gav: Gav): Instant? {
        if (gav in failures) error("simulated fetch failure for $gav")
        return publishTimes[gav]
    }
}
