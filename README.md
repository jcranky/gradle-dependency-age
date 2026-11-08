# gradle-dependency-age

Gradle plugins that gate dependency changes by **publication age** — a defense against supply-chain attacks that ship malicious artifacts through fresh releases (typosquats, compromised maintainer accounts, hijacked builds).

Checksum/PGP dependency verification (Gradle's `--write-verification-metadata`) protects against *tampered files at known coordinates*. It cannot catch a *legitimately published, freshly malicious version* — that is the gap this plugin closes: nothing younger than `minDepAgeDays` passes.

## Plugins (one repo, two ids)

### `com.jcranky.dependency-age-verify` — CI gate

Registers the `verifyDependencyAge` task (group `verification`):

1. Parses the current `gradle.lockfile` + `buildscript-gradle.lockfile`
2. Diffs them against the lockfiles at the base ref (default `origin/main`, or a CI-provided diff base)
3. Looks up publish times for every *changed* GAV (Maven Central `maven-metadata.xml`, falling back to the Solr search API)
4. Fails if any changed GAV was published more recently than `minDepAgeDays`

Semantics worth knowing:

- **Diff-based, not full-scan**: only dependencies *new or changed vs the base ref* are checked — the whole lockfile isn't re-verified on every run
- **Blank baseRef is refused, not inferred**: if no base ref can be determined, the task fails instead of silently scanning everything (bypass protection)
- **Fails closed on unknown publish times**: if a changed GAV's publication date cannot be determined (coordinates unresolvable, lookups failing), the task fails instead of skipping it — an unverifiable dependency counts as potentially fresh
- **Escape hatch** (documented in the failure message): `-PminDepAgeDays=0` for genuine emergencies — note it cannot override the fail-closed case, since there is no age to waive
- Publish-time lookups go through a **shared 30-day cache** (`<buildDir>/dependency-age-cache.json`) to keep CI fast; the cache file doubles as the task's output artifact

### `com.jcranky.dependency-age-filter` — update-report filter

Registers the `filterCandidatesByAge` task (group `verification`): parses a ben-manes `dependencyUpdates` report, rejects candidates younger than `minDepAgeDays`, and writes the filtered copy to `<buildDir>/dependencyUpdates/report-filtered.txt` (section: "Accepted candidates"). The reject-vs-accept data is intentionally staged so update tooling can annotate dropped candidates ("filtered: published 4 days ago — will age out"). Candidates whose publication date cannot be determined land in a separate "Unverifiable candidates" section marked `# UNVERIFIED` and are never recommended as safe.

Both plugins share the same cache file, so a filter run warms the verify gate.

## Usage

```kotlin
// build.gradle.kts
plugins {
    id("com.jcranky.dependency-age-verify") version "0.1.0"
}
```

Prerequisite for the verify gate (locks make the diff meaningful):

```kotlin
// build.gradle.kts
buildscript { configurations.classpath { resolutionStrategy.activateDependencyLocking() } }

dependencyLocking { lockAllConfigurations() }
```

Requires `mavenCentral()` in `pluginManagement` repositories (`settings.gradle.kts`) until the plugins are also published to the Gradle Plugin Portal.

### Configuration

| Property (all tasks) | Default | Meaning |
|---|---|---|
| `minDepAgeDays` | `7` | Minimum age of a candidate/changed GAV; `0` disables the gate |
| `baseRef` (verify only) | `CI_MERGE_REQUEST_DIFF_BASE_SHA` env var, else `origin/main` | Git ref whose lockfiles form the diff base |

All task properties (`mainLockfile`, `buildscriptLockfile`, `cacheFile`, `dependencyUpdatesReport`, `filteredReport`) can be overridden in custom task configuration if your layout differs.

### CI wiring

GitHub Actions (PR-only check):

```yaml
- run: ./gradlew verifyDependencyAge -PbaseRef=${{ github.event.pull_request.base.sha }}
```

GitLab MR pipelines can map the diff base directly: `-PbaseRef=$CI_MERGE_REQUEST_DIFF_BASE_SHA`.

## Where this fits

Layered fresh-release defenses (the npm side of a mixed stack gets the same treatment natively via `.npmrc` `min-release-age`):

| Layer | Tool |
|---|---|
| Decision-time filter (bump proposals) | this repo's *filter* plugin or Renovate's `minimumReleaseAge` |
| CI/merge-time gate (any lockfile writer) | this repo's *verify* plugin — no native Gradle equivalent exists |
| Install-time filter (resolution) | package-manager-native (`npm`/`pnpm`); **nothing exists for Gradle** — which is why the CI gate above matters |

Renovate's own docs recommend pairing bot-side `minimumReleaseAge` with package-manager-level enforcement; for Gradle, this verify gate is that missing layer.

## Development

```bash
./gradlew test
```

New task-flavoured code follows a test-first workflow: existing behaviour is covered by unit tests around the parser/differ/filter/verifier units, and plugin registration is covered by `DependencyAgePluginsTest`; `DependencyAgeSelfVerificationTest` dogfoods the plugins against this repo's own lockfiles through TestKit.

This repo eats its own cooking: its dependencies are locked (`dependencyLocking`, committed `gradle.lockfile` / `buildscript-gradle.lockfile`) so the builds that produce the plugins are reproducible too. Regenerate the lockfiles with `./gradlew :dependencies --write-locks` after dependency changes, and commit them in the same PR — CI fails if they drift.
