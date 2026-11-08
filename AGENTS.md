# gradle-dependency-age — agent instructions

Standalone Gradle plugin repo (published to Maven Central via tagged releases).

## Commands

- `./gradlew test`: run all tests
- `./gradlew :dependencies --write-locks`: regenerate `gradle.lockfile` / `buildscript-gradle.lockfile` after changing dependencies (commit the regenerated files in the same PR; CI fails if they drift)

## Coding standards

- **TDD (MANDATORY)**: never write implementation before a failing test (red → green → refactor)
- **NO suppressions**: no `@Suppress` or lint-skip annotations; fix root causes (refactor, extract, rename) instead
- Kotlin; JVM toolchain 25 (`kotlin-dsl`)
- Task code stays framework-light: units (parsers, differs, filters) are pure Kotlin; Gradle API surface lives in the tasks and plugin entry points
- The repo's own dependencies are locked (`dependencyLocking` in `build.gradle.kts`); a supply-chain tool must have a reproducible dependency graph

## Structure

- `src/main/kotlin/com/jcranky/dependencyage/`: task + plugin entry points, then the pure core (parsers, differ, filter, verifier, fetchers, cache)
- `src/test/kotlin/com/jcranky/dependencyage/`: one test class per unit + `DependencyAgePluginsTest` for registration defaults + `DependencyAgeSelfVerificationTest` (TestKit dogfooding against this repo's own lockfiles)
- Publishing: tag-triggered CI job publishes to Maven Central (Central Portal); version lives in `build.gradle.kts`

## Release notes guidance

Bump versions deliberately: consumers pin plugin versions; changes must ship as new versions with a clean changelog entry in the release notes. New behaviour needs new tests first — existing behaviour is already covered by unit tests (one per unit) plus `DependencyAgePluginsTest` for registration defaults.
