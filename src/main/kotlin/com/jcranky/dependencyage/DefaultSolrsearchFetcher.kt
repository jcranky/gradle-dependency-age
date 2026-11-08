package com.jcranky.dependencyage

import java.net.URLEncoder
import java.time.Instant

class DefaultSolrsearchFetcher(
    private val http: HttpFetcher,
    private val baseUrl: String = "https://search.maven.org",
) : SolrsearchFetcher {
    override fun fetch(gav: Gav): Instant? {
        val query =
            "q=g:%22${URLEncoder.encode(gav.group, Charsets.UTF_8)}%22%20AND%20" +
                "a:%22${URLEncoder.encode(gav.artifact, Charsets.UTF_8)}%22%20AND%20" +
                "v:%22${URLEncoder.encode(gav.version, Charsets.UTF_8)}%22" +
                "&core=gav&rows=1&wt=json"
        val body = http.get("$baseUrl/solrsearch/select?$query") ?: return null
        val match = TIMESTAMP_REGEX.find(body) ?: return null
        return parseEpochMillis(match.groupValues[1])
    }

    private fun parseEpochMillis(raw: String): Instant? =
        runCatching {
            Instant.ofEpochMilli(raw.toLong())
        }.getOrNull()

    private companion object {
        private val TIMESTAMP_REGEX = Regex(""""timestamp"\s*:\s*(\d+)""")
    }
}
