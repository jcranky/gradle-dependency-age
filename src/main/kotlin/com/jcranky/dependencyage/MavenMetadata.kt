package com.jcranky.dependencyage

import java.time.Instant

data class MavenMetadata(
    val latest: String?,
    val lastUpdated: Instant?,
    val versionedLastUpdated: Map<String, Instant> = emptyMap(),
)

interface MavenMetadataFetcher {
    fun fetch(
        group: String,
        artifact: String,
    ): MavenMetadata?
}

interface SolrsearchFetcher {
    fun fetch(gav: Gav): Instant?
}
