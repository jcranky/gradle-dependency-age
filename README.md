# gradle-dependency-age

Gradle plugins that gate dependency updates by **publication age** — designed to defend against supply-chain attacks that ship malicious artifacts through fresh releases (typosquats, compromised accounts, hijacked builds), which checksum verification cannot catch.

- `com.jcranky.dependency-age-verify` — CI-facing gate: diffs the Gradle lockfile against a base ref (`origin/main` by default), looks up publish times for changed GAVs, fails when any is younger than `minDepAgeDays` (default 7).
- `com.jcranky.dependency-age-filter` — update-report filter: takes a ben-manes `dependencyUpdates` report and writes an age-filtered copy, so candidate lists already exclude too-fresh versions.

Status: **work in progress** — code is being extracted from the stonks project buildSrc (where it has been battle-tested) into this standalone, open-source home. Full usage docs land with the first release.
