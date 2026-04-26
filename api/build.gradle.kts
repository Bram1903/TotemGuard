import org.gradle.jvm.tasks.Jar

plugins {
    java
    `maven-publish`
    id("tg-version")
}

val apiBaseVersion = "1.0.0"
val apiSnapshot = rootProject.extra["snapshot"] as Boolean
val apiGitHash = rootProject.extra["gitHash"] as String?

version = buildString {
    append(apiBaseVersion)
    if (apiSnapshot) {
        if (!apiGitHash.isNullOrBlank()) append("+").append(apiGitHash)
        append("-SNAPSHOT")
    }
}

tgVersion {
    packageName.set("com.deathmotion.totemguard.api3.versioning")
    className.set("TGAPIVersions")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 21
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-g")
    sequenceOf("unchecked", "deprecation", "removal").forEach { options.compilerArgs.add("-Xlint:$it") }
}

tasks.named<Jar>("jar") {
    archiveFileName = "${rootProject.name}API-${project.version}.jar"
    archiveClassifier = null
}

tasks.named<Jar>("javadocJar") {
    archiveFileName.set("${rootProject.name}API-${project.version}-javadoc.jar")
}

tasks.named<Jar>("sourcesJar") {
    archiveFileName.set("${rootProject.name}API-${project.version}-sources.jar")
}

tasks.named<Javadoc>("javadoc") {
    title = "TotemGuard API v${project.version}"
    options.encoding = Charsets.UTF_8.name()
    options {
        (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
    }
}

publishing {
    publications {
        create<MavenPublication>("api") {
            groupId = "com.deathmotion"
            artifactId = "${rootProject.name}-api".lowercase()
            version = project.version.toString()
            from(components["java"])

            pom {
                name = "${rootProject.name}API"
                description = "TotemGuard API"
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

configurations.configureEach {
    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
}
