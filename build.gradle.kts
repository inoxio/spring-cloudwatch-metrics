import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.test.logger)
    alias(libs.plugins.jreleaser)
    id("java-library")
    id("maven-publish")
}

group = "de.inoxio"
version = "3.1.0"
description = "A java-spring library to push metrics to cloudwatch"

repositories {
    mavenCentral()
}

dependencies {
    // spring: only needed to compile the auto-configuration, consumers bring their own spring boot
    compileOnly(platform(libs.spring.boot.dependencies))
    compileOnly(libs.spring.boot.autoconfigure)
    // aws
    api(libs.aws.cloudwatch)
    // json, used internally for the dashboard body only
    implementation(libs.jackson.databind)
    // logging
    implementation(libs.slf4j.api)

    // test
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.equalsverifier)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
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
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("central-release"))
        }
    }
}

jreleaser {
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/central-release")
                }
            }
        }
    }
    release {
        github {
            repoOwner = "inoxio"
            name = project.name
            tagName = "{{projectVersion}}"
            releaseName = "{{projectVersion}}"
            branch = "master"
            changelog {
                enabled = false
            }
            releaseNotes {
                enabled = true
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
    withType<Test> {
        useJUnitPlatform()
    }
    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        (options as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
    }
}
