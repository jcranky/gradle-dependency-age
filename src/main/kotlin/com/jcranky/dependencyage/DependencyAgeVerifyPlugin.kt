package com.jcranky.dependencyage

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `verifyDependencyAge` task with parity to the historical
 * stonks-era configuration:
 *  - minAgeDays: gradle property `minDepAgeDays`, default 7
 *  - baseRef: gradle property `baseRef`, then env `CI_MERGE_REQUEST_DIFF_BASE_SHA`,
 *    then `origin/main`
 *  - lockfiles: `gradle.lockfile` and `buildscript-gradle.lockfile` in the project dir
 *  - cache: `<buildDir>/dependency-age-cache.json`
 */
open class DependencyAgeVerifyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("verifyDependencyAge", VerifyDependencyAgeTask::class) { task ->
            task.minAgeDays.set(
                project.providers
                    .gradleProperty("minDepAgeDays")
                    .orElse("7")
                    .map(String::toInt),
            )
            task.baseRef.set(
                project.providers
                    .gradleProperty("baseRef")
                    .orElse(project.providers.environmentVariable("CI_MERGE_REQUEST_DIFF_BASE_SHA"))
                    .orElse("origin/main"),
            )
            task.mainLockfile.set(project.layout.projectDirectory.file("gradle.lockfile"))
            task.buildscriptLockfile.set(project.layout.projectDirectory.file("buildscript-gradle.lockfile"))
            task.cacheFile.set(project.layout.buildDirectory.file("dependency-age-cache.json"))
        }
    }
}
