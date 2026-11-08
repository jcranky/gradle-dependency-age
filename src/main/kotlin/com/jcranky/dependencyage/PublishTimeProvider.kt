package com.jcranky.dependencyage

import java.time.Instant

interface PublishTimeProvider {
    fun getPublishTime(gav: Gav): Instant?
}
