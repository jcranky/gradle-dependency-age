package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class DefaultMavenMetadataFetcherTest {
    @Test
    fun `fetches and parses maven metadata via injected http fetcher`() {
        val xml =
            """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>com.example</groupId>
  <artifactId>lib</artifactId>
  <versioning>
    <latest>1.0.0</latest>
    <lastUpdated>20250601000000</lastUpdated>
  </versioning>
</metadata>"""
        val expectedUrl = "https://repo1.maven.org/maven2/com/example/lib/maven-metadata.xml"
        val http = FakeHttpFetcher(mapOf(expectedUrl to xml))
        val fetcher = DefaultMavenMetadataFetcher(http)

        val result = fetcher.fetch("com.example", "lib")

        assertEquals("1.0.0", result?.latest)
        assertEquals(Instant.parse("2025-06-01T00:00:00Z"), result?.lastUpdated)
        assertEquals(expectedUrl, http.lastRequestedUrl)
    }

    @Test
    fun `returns null when http fetcher returns null`() {
        val http = FakeHttpFetcher(emptyMap())
        val fetcher = DefaultMavenMetadataFetcher(http)

        assertNull(fetcher.fetch("com.example", "lib"))
    }

    @Test
    fun `returns empty metadata when xml is malformed`() {
        val expectedUrl = "https://repo1.maven.org/maven2/com/example/lib/maven-metadata.xml"
        val http = FakeHttpFetcher(mapOf(expectedUrl to "not-xml"))
        val fetcher = DefaultMavenMetadataFetcher(http)

        val result = fetcher.fetch("com.example", "lib")
        assertEquals(null, result?.latest)
        assertEquals(null, result?.lastUpdated)
        assertEquals(emptyMap<String, Instant>(), result?.versionedLastUpdated)
    }

    @Test
    fun `converts group dots to slashes in path`() {
        val expectedUrl = "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/maven-metadata.xml"
        val http = FakeHttpFetcher(mapOf(expectedUrl to "<metadata></metadata>"))
        val fetcher = DefaultMavenMetadataFetcher(http)

        fetcher.fetch("org.junit.jupiter", "junit-jupiter-api")

        assertEquals(expectedUrl, http.lastRequestedUrl)
    }
}

class DefaultSolrsearchFetcherTest {
    @Test
    fun `extracts timestamp from solrsearch response`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val epochMillis = Instant.parse("2025-06-01T00:00:00Z").toEpochMilli()
        val body = """{"response":{"docs":[{"id":"com.example:lib:1.0.0","timestamp":$epochMillis}]}}"""
        val expectedUrl =
            "https://search.maven.org/solrsearch/select?q=g:%22com.example%22%20AND%20" +
                "a:%22lib%22%20AND%20v:%221.0.0%22&core=gav&rows=1&wt=json"
        val http = FakeHttpFetcher(mapOf(expectedUrl to body))

        val fetcher = DefaultSolrsearchFetcher(http)

        assertEquals(Instant.parse("2025-06-01T00:00:00Z"), fetcher.fetch(gav))
    }

    @Test
    fun `returns null when response has no docs`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val body = """{"response":{"docs":[]}}"""
        val http = FakeHttpFetcher(mapOf(".*".toRegex() to body))
        val fetcher = DefaultSolrsearchFetcher(http)

        assertNull(fetcher.fetch(gav))
    }

    @Test
    fun `returns null when http fetcher returns null`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val http = FakeHttpFetcher(emptyMap())
        val fetcher = DefaultSolrsearchFetcher(http)

        assertNull(fetcher.fetch(gav))
    }

    @Test
    fun `returns null when response has no timestamp field`() {
        val gav = Gav("com.example", "lib", "1.0.0")
        val body = """{"response":{"docs":[{"id":"com.example:lib:1.0.0"}]}}"""
        val http = FakeHttpFetcher(mapOf(".*".toRegex() to body))
        val fetcher = DefaultSolrsearchFetcher(http)

        assertNull(fetcher.fetch(gav))
    }
}

private class FakeHttpFetcher(
    private val responses: Map<Any, String>,
) : HttpFetcher {
    var lastRequestedUrl: String? = null
        private set

    override fun get(url: String): String? {
        lastRequestedUrl = url
        return responses.entries.firstOrNull { (key, _) ->
            when (key) {
                is String -> key == url
                is Regex -> key.matches(url)
                else -> false
            }
        }?.value
    }
}
