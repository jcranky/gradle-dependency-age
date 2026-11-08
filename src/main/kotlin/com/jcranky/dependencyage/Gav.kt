package com.jcranky.dependencyage

data class Gav(
    val group: String,
    val artifact: String,
    val version: String,
) {
    override fun toString(): String = "$group:$artifact:$version"

    companion object {
        fun parse(coord: String): Gav {
            val parts = coord.split(":")
            require(parts.size == 3) { "Invalid GAV: $coord (expected group:artifact:version)" }
            return Gav(parts[0], parts[1], parts[2])
        }
    }
}
