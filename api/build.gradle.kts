import java.nio.file.Files
import kotlin.streams.asSequence

plugins {
    totemguard.`java-conventions`
    `maven-publish`
}

version = "1.0.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

val forbidLombokAccessors by tasks.registering {
    group = "verification"
    description = "Fails the build if Lombok @Getter/@Setter is used in main sources."

    val mainJava = project.layout.projectDirectory.dir("src/main/java")
    inputs.dir(mainJava)

    doLast {
        val root = mainJava.asFile.toPath()
        if (!Files.exists(root)) return@doLast

        val forbidden = Regex("""(?m)^\s*@(?:lombok\.)?(Getter|Setter)\b""")

        val violations: List<String> = Files.walk(root).use { paths ->
            paths.asSequence()
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".java") }.mapNotNull { path ->
                    val text = Files.readString(path)
                    if (forbidden.containsMatchIn(text)) path.toString() else null
                }
                .toList()
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Forbidden Lombok annotations found (@Getter/@Setter). Remove them from the API module:")
                    violations.forEach { appendLine(" - $it") }
                }
            )
        }
    }
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
        dependsOn(forbidLombokAccessors)
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
