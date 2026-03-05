plugins {
    `java-library`
    `maven-publish`
}

group = "com.geo.sdk"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
    }
    test {
        java.srcDirs("src/test/java")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.geo.sdk"
            artifactId = "geo-event-trigger-core-sdk"
            version = "0.1.0"
        }
    }
}
