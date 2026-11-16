package com.jcranky.dependencyage

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `verifyDependencyAge` task with parity to the historical configuration:
 *  - minAgeDays: gradle property `minDepAgeDays`, default 7
 *  - baseRef: gradle property `baseRef`, then a CI-provided diff base
 *    (e.g. `CI_MERGE_REQUEST_DIFF_BASE_SHA` in GitLab MR pipelines), then `origin/main`
 *  - lockfiles: `gradle.lockfile` and `buildscript-gradle.lockfile` in the project dir
 *  - cache: `<buildDir>/dependency-age-cache.json`
 */
open class DependencyAgeVerifyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks
            .register("verifyDependencyAge", VerifyDependencyAgeTask::class.java)
            .configure {
                minAgeDays.set(minAgeDaysProperty(project))
                baseRef.set(
                    project.providers
                        .gradleProperty("baseRef")
                        .orElse(project.providers.environmentVariable("CI_MERGE_REQUEST_DIFF_BASE_SHA"))
                        .orElse("origin/main"),
                )
                mainLockfile.set(project.layout.projectDirectory.file("gradle.lockfile"))
                buildscriptLockfile.set(project.layout.projectDirectory.file("buildscript-gradle.lockfile"))
                cacheFile.set(project.layout.buildDirectory.file("dependency-age-cache.json"))
                rootDir.set(project.rootDir)
            }
    }
}
