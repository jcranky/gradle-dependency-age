# Contributing

## Pull request titles follow Conventional Commits

This repo uses **squash merges only**: the PR title becomes the commit on
`main`, and commits on `main` become the changelog. The release tooling
(Release Please) parses PR titles to decide the next version and to group
changelog entries — a non-conforming title silently drops out of the release
notes.

Use these prefixes in PR titles:

- `feat:` — new behaviour → bumps the minor version
- `fix:` — bug fix → bumps the patch version
- `chore:`, `docs:`, `refactor:`, `test:`, `perf:` — no version bump on their own
- Breaking changes: add `!` after the type (e.g. `feat!:`) or a
  `BREAKING CHANGE:` footer in the PR body — bumps the major version and
  surfaces the change prominently in the changelog

Example: `fix: metadata pairing when a version exists in multiple repos`

## Release process

1. Merge conventional PRs to `main`.
2. **Release Please** maintains a single standing release PR containing the
   version bump (`build.gradle.kts`) and the proposed `CHANGELOG.md` entries,
   generated from merged PR titles.
3. **The release PR is a proposal — edit it.** Reword entries, delete entries
   that don't belong, restructure as needed. Nothing reaches the changelog
   that a human didn't merge.
4. Merging the release PR is the release: it tags `vX.Y.Z` and creates the
   GitHub Release with your edited notes.
5. The tag push triggers the `publish` workflow, which signs and uploads to
   Maven Central (Central Portal).

### One-time setup note

The `release-please` workflow needs a `RELEASE_PAT` repo secret — a
fine-grained PAT with `contents: read/write` on this repo. Tags and releases
created with the default `GITHUB_TOKEN` do not trigger other workflows
(GitHub's loop-prevention), so the PAT is what makes the release tag fire
`publish.yml`.

## Verification

- `./gradlew test` — all tests must pass
- `./gradlew :dependencies --write-locks` must be a no-op (lockfile drift fails CI);
  if you changed dependencies, commit the regenerated lockfiles in the same PR
