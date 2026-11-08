package com.jcranky.dependencyage

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DefaultHttpFetcher(
    private val requestTimeout: Duration = Duration.ofSeconds(5),
    private val client: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(requestTimeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build(),
) : HttpFetcher {
    override fun get(url: String): String? {
        return runCatching {
            val request =
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)
                    .GET()
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) response.body() else null
        }.getOrNull()
    }
}
