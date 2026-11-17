package com.jcranky.dependencyage

import java.io.File
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DependencyAgePluginsTest {

    private fun minimalProject(): Project =
        ProjectBuilder.builder().build()

    @Test
    fun verifyPluginRegistersVerifyTaskWithDefaults() {
        val project = minimalProject()

        project.pluginManager.apply(DependencyAgeVerifyPlugin::class.java)

        val task = project.tasks.named("verifyDependencyAge", VerifyDependencyAgeTask::class.java).get()
        assertEquals(7, task.minAgeDays.get())
        assertEquals("origin/main", task.baseRef.get())
        assertEquals("gradle.lockfile", task.mainLockfile.get().asFile.name)
        assertEquals("buildscript-gradle.lockfile", task.buildscriptLockfile.get().asFile.name)
        assertEquals(
            project.layout.buildDirectory.file("dependency-age-cache.json").get(),
            task.cacheFile.get(),
        )
        assertEquals("verification", task.group)
    }

    @Test
    fun verifyPluginHonoursGradleProperties() {
        val projectDir = File(javaClass.getResource("/plugin-props")!!.file)
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()

        project.pluginManager.apply(DependencyAgeVerifyPlugin::class.java)

        val task = project.tasks.named("verifyDependencyAge", VerifyDependencyAgeTask::class.java).get()
        assertEquals(3, task.minAgeDays.get())
        assertEquals("origin/some-branch", task.baseRef.get())
    }

    @Test
    fun filterPluginRegistersFilterTaskWithDefaults() {
        val project = minimalProject()

        project.pluginManager.apply(DependencyAgeFilterPlugin::class.java)

        val task = project.tasks.named("filterCandidatesByAge", FilterCandidatesByAgeTask::class.java).get()
        assertEquals(7, task.minAgeDays.get())
        assertEquals(
            project.layout.buildDirectory.file("dependencyUpdates/report.txt").get(),
            task.dependencyUpdatesReport.get(),
        )
        assertEquals(
            project.layout.buildDirectory.file("dependencyUpdates/report-filtered.txt").get(),
            task.filteredReport.get(),
        )
        assertEquals(
            project.layout.buildDirectory.file("dependency-age-cache.json").get(),
            task.cacheFile.get(),
        )
        assertEquals("verification", task.group)
    }

    @Test
    fun tasksAreRegisteredWithoutExecutingThem() {
        val project = minimalProject()
        project.pluginManager.apply(DependencyAgeVerifyPlugin::class.java)
        project.pluginManager.apply(DependencyAgeFilterPlugin::class.java)

        // Registration must be lazy: realizing the tasks (configuration only)
        // must not throw — execution failure requires an actual run with
        // real lockfile/report inputs.
        assertNotNull(project.tasks.findByName("verifyDependencyAge"))
        assertNotNull(project.tasks.findByName("filterCandidatesByAge"))
    }
}
