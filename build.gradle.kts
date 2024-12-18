import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.adarshr.test-logger") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.0"
    id("java-library")
    id("maven-publish")
    id("org.springframework.boot") version "3.4.0"
}

group = "de.inoxio"
version = "2.0.0"
description = "A java-spring library to push metrics to cloudwatch"

repositories {
    mavenCentral()
}

dependencies {
    // spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    // aws
    implementation("software.amazon.awssdk:cloudwatch:2.29.36")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.17.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
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
    }
}
