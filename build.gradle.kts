plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
}

group = "com.jcranky"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        create("dependencyAgeVerify") {
            id = "com.jcranky.dependency-age-verify"
            implementationClass = "com.jcranky.dependencyage.DependencyAgeVerifyPlugin"
            displayName = "Dependency Age Verify"
            description = "Fails the build when the lockfile diff vs a base ref contains dependencies published more recently than minDepAgeDays."
            tags.set(listOf("supply-chain-security", "dependency-management", "verification"))
        }
        create("dependencyAgeFilter") {
            id = "com.jcranky.dependency-age-filter"
            implementationClass = "com.jcranky.dependencyage.DependencyAgeFilterPlugin"
            displayName = "Dependency Age Filter"
            description = "Filters ben-manes dependencyUpdates candidates by publication age and writes a filtered report."
            tags.set(listOf("supply-chain-security", "dependency-management"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            url.set("https://github.com/jcranky/gradle-dependency-age")
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("jcranky")
                    name.set("JCranky")
                    url.set("https://github.com/jcranky")
                }
            }
            scm {
                url.set("https://github.com/jcranky/gradle-dependency-age")
                connection.set("scm:git:git://github.com/jcranky/gradle-dependency-age.git")
                developerConnection.set("scm:git:ssh://github.com/jcranky/gradle-dependency-age.git")
            }
        }
    }
}
