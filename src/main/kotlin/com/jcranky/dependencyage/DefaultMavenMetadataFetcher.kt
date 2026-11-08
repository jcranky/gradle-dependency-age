package com.jcranky.dependencyage

class DefaultMavenMetadataFetcher(
    private val http: HttpFetcher = DefaultHttpFetcher(),
    private val parser: MavenMetadataParser = MavenMetadataParser(),
) : MavenMetadataFetcher {
    override fun fetch(
        group: String,
        artifact: String,
    ): MavenMetadata? {
        val path = group.replace('.', '/') + "/" + artifact + "/maven-metadata.xml"
        val xml = http.get("https://repo1.maven.org/maven2/$path") ?: return null
        return runCatching { parser.parse(xml) }.getOrNull()
    }
}
