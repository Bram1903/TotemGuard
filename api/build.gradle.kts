plugins {
    totemguard.`java-conventions`
    `maven-publish`
}

version = "1.0.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    jar {
        archiveFileName = "${rootProject.name}API-${version}.jar"
        archiveClassifier = "default"
    }

    compileJava {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-g")

        sequenceOf("unchecked", "deprecation", "removal").forEach { options.compilerArgs.add("-Xlint:$it") }
    }

    named<Jar>("javadocJar") {
        archiveFileName.set("${rootProject.name}API-${version}-javadoc.jar")
    }

    named<Jar>("sourcesJar") {
        archiveFileName.set("${rootProject.name}API-${version}-sources.jar")
    }

    javadoc {
        title = "TotemGuard API v${version}"
        options.encoding = Charsets.UTF_8.name()
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("api") {
            groupId = project.group as String
            artifactId = "TotemGuardAPI"
            version = version as String
            from(components["java"])

            pom {
                name = "${rootProject.name}API"
                description = rootProject.description
                url = "https://github.com/Bram1903/TotemGuard"

                developers {
                    developer {
                        id = "bram"
                        name = "Bram"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/Bram1903/TotemGuard.git"
                    developerConnection = "scm:git:https://github.com/Bram1903/TotemGuard.git"
                    url = "https://github.com/Bram1903/TotemGuard"
                }
            }
        }
    }

    repositories {
        maven {
            name = "PvPHub"
            url = uri("https://maven.pvphub.me/bram")
            credentials {
                username = System.getenv("PVPHUB_MAVEN_USERNAME")
                password = System.getenv("PVPHUB_MAVEN_SECRET")
            }
        }
    }
}

// So that SNAPSHOT is always the latest SNAPSHOT
configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
}

val taskNames = gradle.startParameter.taskNames
if (taskNames.any { it.contains("build") }
    && taskNames.any { it.contains("publish") }) {
    throw IllegalStateException("Cannot build and publish at the same time.")
}