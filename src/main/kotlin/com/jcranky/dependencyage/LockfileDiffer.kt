package com.jcranky.dependencyage

import java.io.File

object LockfileDiffer {
    sealed class ReadFromBaseResult {
        data class Success(
            val gavs: List<Gav>,
        ) : ReadFromBaseResult()

        data class Failure(
            val reason: String,
            val stderr: String? = null,
        ) : ReadFromBaseResult()
    }

    fun diff(
        current: List<Gav>,
        previous: List<Gav>,
    ): List<Gav> {
        val currSet = current.toSet()
        val prevSet = previous.toSet()
        return ((currSet - prevSet) + (prevSet - currSet)).distinct()
    }

    fun readFromBase(
        baseRef: String,
        mainLockfile: File,
        buildscriptLockfile: File,
        workingDir: File,
    ): ReadFromBaseResult {
        if (baseRef.isBlank()) {
            return ReadFromBaseResult.Failure("baseRef is blank")
        }
        when (val repoLookup = findRepoRoot(workingDir)) {
            is RepoRootResult.Found -> {
                val repoRoot = repoLookup.path
                val mainRel = mainLockfile.absolutePath.removePrefix(repoRoot.absolutePath).trimStart('/')
                val buildscriptRel =
                    buildscriptLockfile.absolutePath.removePrefix(repoRoot.absolutePath).trimStart('/')

                val mainResult = readLockfileAtRef(workingDir, baseRef, mainRel)
                if (mainResult is ReadFromBaseResult.Failure) return mainResult
                val buildscriptResult = readLockfileAtRef(workingDir, baseRef, buildscriptRel)
                if (buildscriptResult is ReadFromBaseResult.Failure) return buildscriptResult

                val gavs = (mainResult as ReadFromBaseResult.Success).gavs +
                    (buildscriptResult as ReadFromBaseResult.Success).gavs
                return ReadFromBaseResult.Success(gavs)
            }
            is RepoRootResult.Failed -> {
                return ReadFromBaseResult.Failure(
                    reason = "could not determine git repo root from $workingDir: ${repoLookup.detail}",
                )
            }
        }
    }

    private sealed class RepoRootResult {
        data class Found(val path: File) : RepoRootResult()
        data class Failed(val detail: String) : RepoRootResult()
    }

    private fun readLockfileAtRef(
        workingDir: File,
        baseRef: String,
        repoRelativePath: String,
    ): ReadFromBaseResult {
        when (
            val result =
                runGitCommand(workingDir, "show", "$baseRef:$repoRelativePath")
        ) {
            is GitCommandResult.Success -> {
                return ReadFromBaseResult.Success(LockfileParser.parseText(result.stdout))
            }
            is GitCommandResult.SpawnFailure -> {
                return ReadFromBaseResult.Failure(
                    reason = "failed to spawn 'git show' for $repoRelativePath at $baseRef",
                    stderr = result.error,
                )
            }
            is GitCommandResult.GitFailure -> {
                if (isLockfileMissingAtRef(result.stderr)) {
                    return ReadFromBaseResult.Success(emptyList())
                }
                val stderrExcerpt = result.stderr.lineSequence().take(5).joinToString("\n").ifBlank { null }
                return ReadFromBaseResult.Failure(
                    reason = "'git show $baseRef:$repoRelativePath' exited with code ${result.exitCode}",
                    stderr = stderrExcerpt,
                )
            }
        }
    }

    private fun findRepoRoot(startDir: File): RepoRootResult =
        when (val result = runGitCommand(startDir, "rev-parse", "--show-toplevel")) {
            is GitCommandResult.Success ->
                RepoRootResult.Found(File(result.stdout.trim()))
            is GitCommandResult.GitFailure ->
                RepoRootResult.Failed(
                    "git rev-parse exited with code ${result.exitCode}: ${result.stderr.lineSequence().firstOrNull()}",
                )
            is GitCommandResult.SpawnFailure ->
                RepoRootResult.Failed("could not spawn git: ${result.error}")
        }

    private fun isLockfileMissingAtRef(stderr: String): Boolean =
        stderr.lineSequence().any { line ->
            line.contains("does not exist") ||
                line.contains("exists on disk, but not in") ||
                line.contains("not in '")
        }

    private sealed class GitCommandResult {
        data class Success(
            val exitCode: Int,
            val stdout: String,
        ) : GitCommandResult()

        data class GitFailure(
            val exitCode: Int,
            val stdout: String,
            val stderr: String,
        ) : GitCommandResult()

        data class SpawnFailure(
            val error: String,
        ) : GitCommandResult()
    }

    private fun runGitCommand(
        workingDir: File,
        vararg args: String,
    ): GitCommandResult =
        try {
            val process =
                ProcessBuilder("git", *args)
                    .directory(workingDir)
                    .redirectErrorStream(false)
                    .start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit == 0) {
                GitCommandResult.Success(exit, stdout)
            } else {
                GitCommandResult.GitFailure(exit, stdout, stderr)
            }
        } catch (e: Exception) {
            GitCommandResult.SpawnFailure(
                "${e.javaClass.simpleName}: ${e.message ?: "(no message)"}",
            )
        }
}
