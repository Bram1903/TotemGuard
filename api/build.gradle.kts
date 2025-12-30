plugins {
    java
    `maven-publish`
}

version = "1.0.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks {
    jar {
        archiveFileName = "${rootProject.name}API-${version}.jar"
        archiveClassifier = null
    }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 8
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
            groupId = "com.deathmotion"
            artifactId = "${rootProject.name}-api".lowercase()
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
                    developer {
                        id = "outdev"
                        name = "OutDev"
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
