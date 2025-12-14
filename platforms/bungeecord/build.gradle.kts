plugins {
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
}

repositories {
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven("https://repo.papermc.io/repository/maven-public/") // For Brigadier
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.bungeecord)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-Bungee-${rootProject.ext["versionNoHash"]}.jar"
        archiveClassifier = null
        destinationDirectory = rootProject.layout.buildDirectory
        exclude("META-INF/maven/**")
    }

    assemble {
        dependsOn(shadowJar)
    }
}
