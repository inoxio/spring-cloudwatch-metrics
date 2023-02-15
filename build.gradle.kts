import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.ben-manes.versions") version "0.45.0"
    id("com.jfrog.bintray") version "1.8.5"
    id("org.springframework.boot") version "2.7.8"
    id("com.adarshr.test-logger") version "3.2.0"
}

group = "de.inoxio"
version = "1.2.0"
description = "A java-spring library to push metrics to cloudwatch"

apply {
    plugin("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    // aws
    implementation("software.amazon.awssdk:cloudwatch:2.20.5")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.13.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn + "classes"
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn + "javadoc"
    archiveClassifier.set("javadoc")
    from(tasks.withType<Javadoc>().first().destinationDir)
}

publishing {
    publications {
        register("publication", MavenPublication::class) {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom.withXml {
                asNode().apply {
                    appendNode("description", project.description)
                    appendNode("name", "$groupId:$artifactId")
                    appendNode("url", "https://github.com/inoxio/${project.name}")

                    val license = appendNode("licenses").appendNode("license")
                    license.appendNode("name", "The Apache Software License, Version 2.0")
                    license.appendNode("url", "http://www.apache.org/licenses/LICENSE-2.0.txt")

                    val developer = appendNode("developers").appendNode("developer")
                    developer.appendNode("name", "Michael Kunze")
                    developer.appendNode("email", "mkunze@inoxio.de")
                    developer.appendNode("organization", "inoxio Quality Services GmbH")
                    developer.appendNode("organizationUrl", "https://www.inoxio.de")

                    val scm = appendNode("scm")
                    scm.appendNode("connection", "scm:git:git://github.com/inoxio/${project.name}.git")
                    scm.appendNode("developerConnection", "scm:git:ssh://github.com:inoxio/${project.name}.git")
                    scm.appendNode("url", "http://github.com/inoxio/${project.name}/tree/master")
                }
            }
        }
    }
}

bintray {
    user = properties["bintray.user"] as String?
            ?: System.getenv("BINTRAY_USER")
    key = properties["bintray.api-key"] as String?
            ?: System.getenv("BINTRAY_API_KEY")

    setPublications("publication")

    publish = true
    override = false

    pkg.apply {
        repo = "maven"
        name = project.name
        userOrg = "inoxio"
        desc = project.description
        websiteUrl = "https://github.com/inoxio/${project.name}"
        issueTrackerUrl = "https://github.com/inoxio/${project.name}/issues"
        vcsUrl = "https://github.com/inoxio/${project.name}.git"
        setLicenses("Apache-2.0")
        githubRepo = "inoxio/${project.name}"

        version.apply {
            name = project.version as String
            vcsTag = project.version as String
            gpg.apply {
                sign = true
            }
            mavenCentralSync.apply {
                sync = true
                user = properties["oss.user"] as String?
                        ?: System.getenv("OSS_USER")
                password = properties["oss.password"] as String?
                        ?: System.getenv("OSS_PASSWORD")
                close = "1"
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.apply {
            isFork = true
            isIncremental = true
            encoding = "UTF-8"
            compilerArgs = mutableListOf("-Xlint")
        }
    }
    withType<Jar> {
        enabled = true
    }
    withType<BootJar> {
        enabled = false
    }
    withType<Test> {
        useJUnitPlatform()
    }
    withType<DependencyUpdatesTask> {
        rejectVersionIf {
            listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "pr")
                    .any { qualifier -> "(?i).*[.-]$qualifier[.\\d-+]*".toRegex().matches(candidate.version) }
        }
    }
    withType<Wrapper> {
        distributionType = ALL
        gradleVersion = "8.0"
    }
    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
