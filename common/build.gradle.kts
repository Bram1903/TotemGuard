plugins {
    `java-library`
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
    `tg-version`
}

dependencies {
    api(project(":api"))
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
}

tasks {
    shadowJar {
        relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
    }

    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.common.util"
    }
}