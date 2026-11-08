package com.jcranky.dependencyage

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the `filterCandidatesByAge` task with parity to the published configuration
 * historical configuration:
 *  - minAgeDays: gradle property `minDepAgeDays`, default 7
 *  - input: `<buildDir>/dependencyUpdates/report.txt` (ben-manes dependencyUpdates report)
 *  - output: `<buildDir>/dependencyUpdates/report-filtered.txt`
 *  - cache: `<buildDir>/dependency-age-cache.json` (shared with the verify plugin)
 */
open class DependencyAgeFilterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks
            .register("filterCandidatesByAge", FilterCandidatesByAgeTask::class.java)
            .configure {
                minAgeDays.set(
                    project.providers
                        .gradleProperty("minDepAgeDays")
                        .orElse("7")
                        .map(String::toInt),
                )
                dependencyUpdatesReport.set(
                    project.layout.buildDirectory.file("dependencyUpdates/report.txt"),
                )
                filteredReport.set(
                    project.layout.buildDirectory.file("dependencyUpdates/report-filtered.txt"),
                )
                cacheFile.set(project.layout.buildDirectory.file("dependency-age-cache.json"))
            }
    }
}
