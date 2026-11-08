package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class HybridPublishTimeProviderTest {
    @Test
    fun `uses lastUpdated from maven metadata when requested version matches latest`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val lastUpdated = Instant.parse("2025-06-01T00:00:00Z")
        val metadata =
            FakeMetadataFetcher(
                mapOf(
                    Pair(gav.group, gav.artifact) to
                        MavenMetadata(latest = gav.version, lastUpdated = lastUpdated),
                ),
            )
        val solrsearch = RecordingSolrsearchFetcher()

        val provider = HybridPublishTimeProvider(metadata, solrsearch)

        assertEquals(lastUpdated, provider.getPublishTime(gav))
        assertEquals(0, solrsearch.callCount, "solrsearch should not be called when metadata has timestamp")
    }

    @Test
    fun `uses lastUpdated from maven metadata when version has explicit timestamp`() {
        val gav = Gav("com.example", "lib", "2.0.0")
        val lastUpdated = Instant.parse("2025-06-01T00:00:00Z")
        val metadata =
            FakeMetadataFetcher(
                mapOf(
                    Pair(gav.group, gav.artifact) to
                        MavenMetadata(
                            latest = "1.0.0",
                            lastUpdated = Instant.parse("2020-01-01T00:00:00Z"),
                            versionedLastUpdated = mapOf(gav.version to lastUpdated),
                        ),
                ),
            )
        val solrsearch = RecordingSolrsearchFetcher()

        val provider = HybridPublishTimeProvider(metadata, solrsearch)

        assertEquals(lastUpdated, provider.getPublishTime(gav))
        assertEquals(0, solrsearch.callCount)
    }

    @Test
    fun `falls back to solrsearch when metadata lacks per-version timestamp`() {
        val gav = Gav("com.example", "lib", "2.0.0")
        val solrsearchTime = Instant.parse("2025-07-01T00:00:00Z")
        val metadata =
            FakeMetadataFetcher(
                mapOf(
                    Pair(gav.group, gav.artifact) to
                        MavenMetadata(
                            latest = "1.0.0",
                            lastUpdated = Instant.parse("2020-01-01T00:00:00Z"),
                            versionedLastUpdated = emptyMap(),
                        ),
                ),
            )
        val solrsearch = RecordingSolrsearchFetcher(mapOf(gav to solrsearchTime))

        val provider = HybridPublishTimeProvider(metadata, solrsearch)

        assertEquals(solrsearchTime, provider.getPublishTime(gav))
        assertEquals(1, solrsearch.callCount)
    }

    @Test
    fun `falls back to solrsearch when metadata fetcher returns null`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val solrsearchTime = Instant.parse("2025-07-01T00:00:00Z")
        val metadata = FakeMetadataFetcher(emptyMap())
        val solrsearch = RecordingSolrsearchFetcher(mapOf(gav to solrsearchTime))

        val provider = HybridPublishTimeProvider(metadata, solrsearch)

        assertEquals(solrsearchTime, provider.getPublishTime(gav))
    }

    @Test
    fun `returns null when both metadata and solrsearch have no answer`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val provider = HybridPublishTimeProvider(
            FakeMetadataFetcher(emptyMap()),
            RecordingSolrsearchFetcher(emptyMap()),
        )

        assertNull(provider.getPublishTime(gav))
    }
}

private class FakeMetadataFetcher(
    private val responses: Map<Pair<String, String>, MavenMetadata>,
) : MavenMetadataFetcher {
    override fun fetch(
        group: String,
        artifact: String,
    ): MavenMetadata? = responses[group to artifact]
}

private class RecordingSolrsearchFetcher(
    private val responses: Map<Gav, Instant> = emptyMap(),
) : SolrsearchFetcher {
    var callCount = 0
        private set

    override fun fetch(gav: Gav): Instant? {
        callCount++
        return responses[gav]
    }
}
