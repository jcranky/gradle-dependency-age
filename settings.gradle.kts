rootProject.name = "gradle-dependency-age"

plugins {
    id("com.gradleup.nmcp.settings").version("1.6.2")
}

// Central Portal credentials come from the environment (CI); locally the
// publish-to-portal tasks fail at upload time unless the env is set.
nmcpSettings {
    centralPortal {
        username = System.getenv("CENTRAL_SONATYPE_TOKEN_USERNAME") ?: ""
        password = System.getenv("CENTRAL_SONATYPE_TOKEN_PASSWORD") ?: ""
    }
}
