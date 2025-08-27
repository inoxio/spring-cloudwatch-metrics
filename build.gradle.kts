import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.adarshr.test-logger") version "4.0.0"
    id("com.rickbusarow.github-release-fork") version "2.5.2"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("java-library")
    id("maven-publish")
    id("org.springframework.boot") version "3.5.5"
    id("signing")
}

group = "de.inoxio"
version = "2.0.1"
description = "A java-spring library to push metrics to cloudwatch"

repositories {
    mavenCentral()
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    // aws
    implementation("software.amazon.awssdk:cloudwatch:2.32.31")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.0.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])

            pom {

                name = "$groupId:$artifactId"
                description = project.description
                url = "https://github.com/inoxio/${project.name}"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Michael Kunze"
                        email = "mkunze@inoxio.de"
                        organization = "inoxio Quality Services GmbH"
                        organizationUrl = "https://www.inoxio.de"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/inoxio/${project.name}.git"
                    developerConnection = "scm:git:ssh://github.com:inoxio/${project.name}.git"
                    url = "http://github.com/inoxio/${project.name}/tree/master"
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

githubRelease {
    token(project.findProperty("githubToken.inoxio") as? String)
    repo = project.name
    owner = "inoxio"
    tagName = project.version as String
    releaseName = project.version as String
    generateReleaseNotes = true
    targetCommitish = "master"
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
    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
    }
}
