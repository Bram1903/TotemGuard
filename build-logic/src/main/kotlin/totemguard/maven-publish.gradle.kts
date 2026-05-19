package totemguard

plugins {
    `maven-publish`
}

val pvpHubUser = providers.environmentVariable("PVPHUB_MAVEN_USERNAME")
val pvpHubPass = providers.environmentVariable("PVPHUB_MAVEN_SECRET")

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
                username = pvpHubUser.orNull
                password = pvpHubPass.orNull
            }
        }
    }
}
