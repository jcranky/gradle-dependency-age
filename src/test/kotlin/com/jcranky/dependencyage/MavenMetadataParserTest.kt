package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class MavenMetadataParserTest {
    @Test
    fun `parses latest version and lastUpdated at versioning level`() {
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.example</groupId>
              <artifactId>lib</artifactId>
              <versioning>
                <latest>2.0.0</latest>
                <release>2.0.0</release>
                <versions>
                  <version>1.0.0</version>
                  <version>2.0.0</version>
                </versions>
                <lastUpdated>20250601000000</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()

        val result = MavenMetadataParser().parse(xml)

        assertEquals("2.0.0", result.latest)
        assertEquals(Instant.parse("2025-06-01T00:00:00Z"), result.lastUpdated)
    }

    @Test
    fun `parses per-version lastUpdated when wrapped in version tags`() {
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.example</groupId>
              <artifactId>lib</artifactId>
              <versioning>
                <latest>2.0.0</latest>
                <versions>
                  <version>
                    <value>1.0.0</value>
                    <lastUpdated>20200101000000</lastUpdated>
                  </version>
                  <version>
                    <value>2.0.0</value>
                    <lastUpdated>20250601000000</lastUpdated>
                  </version>
                </versions>
                <lastUpdated>20250601000000</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()

        val result = MavenMetadataParser().parse(xml)

        assertEquals(
            mapOf(
                "1.0.0" to Instant.parse("2020-01-01T00:00:00Z"),
                "2.0.0" to Instant.parse("2025-06-01T00:00:00Z"),
            ),
            result.versionedLastUpdated,
        )
    }

    @Test
    fun `does not mis-assign versioning-level lastUpdated to the first listed version`() {
        // Standard repo1 maven-metadata.xml: N plain <version> tags followed by a single
        // versioning-level <lastUpdated>. Pairing by position would map the oldest
        // listed version to the most recent update timestamp of the artifact.
        val xml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>com.example</groupId>
              <artifactId>lib</artifactId>
              <versioning>
                <latest>3.0.0</latest>
                <versions>
                  <version>1.0.0</version>
                  <version>2.0.0</version>
                  <version>3.0.0</version>
                </versions>
                <lastUpdated>20250601000000</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()

        val result = MavenMetadataParser().parse(xml)

        assertEquals(emptyMap<String, Instant>(), result.versionedLastUpdated)
    }

    @Test
    fun `pairs versions with adjacent lastUpdated tags in flat format`() {
        val xml =
            """
            <metadata>
              <versioning>
                <versions>
                  <version>1.0.0</version>
                  <lastUpdated>20200101000000</lastUpdated>
                  <version>2.0.0</version>
                  <lastUpdated>20250601000000</lastUpdated>
                </versions>
                <lastUpdated>20250601000000</lastUpdated>
              </versioning>
            </metadata>
            """.trimIndent()

        val result = MavenMetadataParser().parse(xml)

        assertEquals(
            mapOf(
                "1.0.0" to Instant.parse("2020-01-01T00:00:00Z"),
                "2.0.0" to Instant.parse("2025-06-01T00:00:00Z"),
            ),
            result.versionedLastUpdated,
        )
    }

    @Test
    fun `returns nulls when metadata is empty`() {
        val result = MavenMetadataParser().parse("<metadata></metadata>")

        assertNull(result.latest)
        assertNull(result.lastUpdated)
        assertEquals(emptyMap<String, Instant>(), result.versionedLastUpdated)
    }
}
