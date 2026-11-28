# Changelog

## [0.2.1](https://github.com/jcranky/gradle-dependency-age/compare/v0.2.0...v0.2.1) (2026-11-28)


### Bug Fixes

* derive project version from the release-please manifest ([#18](https://github.com/jcranky/gradle-dependency-age/issues/18)) ([fd721cd](https://github.com/jcranky/gradle-dependency-age/commit/fd721cd0d26d086f80ef8fc43b6fd07cbf38ee5f))

## [0.2.0](https://github.com/jcranky/gradle-dependency-age/compare/v0.1.0...v0.2.0) (2026-11-28)


### Features

* default minimum publication age to 1 day ([#16](https://github.com/jcranky/gradle-dependency-age/issues/16)) ([a934c43](https://github.com/jcranky/gradle-dependency-age/commit/a934c43aaf7286f743692aba8611f8b193662d2f))

## 0.1.0 (2026-11-27)


### Features

* extract dependency-age core and plugin entrypoints from battle-tested buildSrc ([#1](https://github.com/jcranky/gradle-dependency-age/issues/1)) ([f135290](https://github.com/jcranky/gradle-dependency-age/commit/f135290bfeef18fd34a899e142336eee02de5350))
* publish to Maven Central via Central Portal (nmcp) ([#7](https://github.com/jcranky/gradle-dependency-age/issues/7)) ([97687a6](https://github.com/jcranky/gradle-dependency-age/commit/97687a661484d739325efb740a65125f50de38c7))
* release automation via Release Please ([#10](https://github.com/jcranky/gradle-dependency-age/issues/10)) ([83ec52d](https://github.com/jcranky/gradle-dependency-age/commit/83ec52d9b2c10624d439a04babbbdd70e9d3cff4))


### Bug Fixes

* add POM name/description for Central validation; bump Actions to node24 majors ([#9](https://github.com/jcranky/gradle-dependency-age/issues/9)) ([4370607](https://github.com/jcranky/gradle-dependency-age/commit/4370607931d83d8fe832f50a8a3cab9a265eb815))
* bump release-please-action to v5 (node24 native) ([#14](https://github.com/jcranky/gradle-dependency-age/issues/14)) ([3d3d2ba](https://github.com/jcranky/gradle-dependency-age/commit/3d3d2ba7e7a194c383c0d5400e2a05a89aa98fbf))
* first release proposes 0.1.0; move to googleapis/release-please-action ([#12](https://github.com/jcranky/gradle-dependency-age/issues/12)) ([b3309d3](https://github.com/jcranky/gradle-dependency-age/commit/b3309d3e55bc28b52c5c0129f85adfc99dd7bc37))
* review findings from PR [#1](https://github.com/jcranky/gradle-dependency-age/issues/1) — metadata pairing, config cache, cache races, report formatting ([#5](https://github.com/jcranky/gradle-dependency-age/issues/5)) ([6913209](https://github.com/jcranky/gradle-dependency-age/commit/69132095a8e5614fd1ece913386d2b8afb2aa752))
* unknown publish times no longer pass silently (verify gate fails closed, filter stops recommending) ([#4](https://github.com/jcranky/gradle-dependency-age/issues/4)) ([456e7be](https://github.com/jcranky/gradle-dependency-age/commit/456e7beb2ae697d00cdd1c3c3919cdf7016d4f4c))
