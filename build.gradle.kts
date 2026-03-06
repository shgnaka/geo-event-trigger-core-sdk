plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "com.geo.sdk"

val artifactVersion = providers
    .gradleProperty("artifactVersion")
    .orElse(providers.environmentVariable("ARTIFACT_VERSION"))
    .orElse("0.1.0")

version = artifactVersion.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
    }
    test {
        java.srcDirs("src/test/java")
    }
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.geo.sdk"
            artifactId = "geo-event-trigger-core-sdk"
            version = artifactVersion.get()

            pom {
                name.set("geo-event-trigger-core-sdk")
                description.set("Context intervention core SDK")
                url.set("https://github.com/shgnaka/geo-event-trigger-core-sdk")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("shgnaka")
                        name.set("shgnaka")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/shgnaka/geo-event-trigger-core-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/shgnaka/geo-event-trigger-core-sdk.git")
                    url.set("https://github.com/shgnaka/geo-event-trigger-core-sdk")
                }
            }
        }
    }

    repositories {
        maven {
            name = "central"
            val releaseRepoUrl = providers
                .gradleProperty("centralRepoUrl")
                .orElse(providers.environmentVariable("CENTRAL_REPOSITORY_URL"))
                .orElse("https://central.sonatype.com/api/v1/publisher/upload")
            url = uri(releaseRepoUrl.get())
            credentials {
                username = providers
                    .gradleProperty("centralPortalUsername")
                    .orElse(providers.environmentVariable("CENTRAL_PORTAL_USERNAME"))
                    .orNull
                password = providers
                    .gradleProperty("centralPortalPassword")
                    .orElse(providers.environmentVariable("CENTRAL_PORTAL_PASSWORD"))
                    .orNull
            }
        }
    }
}

signing {
    val signingKey = providers
        .gradleProperty("signingKey")
        .orElse(providers.environmentVariable("SIGNING_KEY"))
        .orNull
    val signingPassword = providers
        .gradleProperty("signingPassword")
        .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
        .orNull
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}
