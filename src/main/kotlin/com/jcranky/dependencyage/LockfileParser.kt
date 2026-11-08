package com.jcranky.dependencyage

import java.io.File

object LockfileParser {
    fun parse(file: File): List<Gav> =
        file.useLines { lines -> parseLines(lines) }

    fun parseText(text: String): List<Gav> = parseLines(text.lineSequence())

    private fun parseLines(lines: Sequence<String>): List<Gav> =
        lines
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val coord = line.substringBefore("=")
                runCatching { Gav.parse(coord) }.getOrNull()
            }.toList()
}
