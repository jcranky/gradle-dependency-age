package com.jcranky.dependencyage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class LockfileParserTest {
    @Test
    fun `parse extracts gav coordinates from lockfile entries`() {
        val gavs = LockfileParser.parse(File("src/test/resources/lockfile-sample.txt"))

        assertEquals(
            listOf(
                Gav("com.auth0", "java-jwt", "4.6.1"),
                Gav("at.favre.lib", "bcrypt", "0.10.2"),
                Gav("ch.qos.logback", "logback-classic", "1.6.3"),
            ),
            gavs,
        )
    }

    @Test
    fun `parse skips blank lines and comments`() {
        val file = File.createTempFile("lockfile", ".txt").also { it.deleteOnExit() }
        file.writeText(
            """
            # This is a Gradle generated file for dependency locking.

            # A comment line above
            com.example:lib:1.0.0=compileClasspath

            # Another comment
            com.example:lib:2.0.0=testRuntimeClasspath
            """.trimIndent(),
        )

        val gavs = LockfileParser.parse(file)

        assertEquals(
            listOf(
                Gav("com.example", "lib", "1.0.0"),
                Gav("com.example", "lib", "2.0.0"),
            ),
            gavs,
        )
    }
}
