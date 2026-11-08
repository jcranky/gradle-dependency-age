package com.jcranky.dependencyage

interface HttpFetcher {
    fun get(url: String): String?
}
