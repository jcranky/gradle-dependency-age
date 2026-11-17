package com.jcranky.dependencyage

import java.time.Instant

class HybridPublishTimeProvider(
    private val metadataFetcher: MavenMetadataFetcher,
    private val solrsearchFetcher: SolrsearchFetcher,
) : PublishTimeProvider {
    override fun getPublishTime(gav: Gav): Instant? {
        val metadata = metadataFetcher.fetch(gav.group, gav.artifact)
        if (metadata != null) {
            val versioned = metadata.versionedLastUpdated[gav.version]
            if (versioned != null) return versioned
            if (metadata.latest == gav.version && metadata.lastUpdated != null) {
                return metadata.lastUpdated
            }
        }
        return solrsearchFetcher.fetch(gav)
    }
}
