plugins {
    totemguard.`java-conventions`
    `tg-version`
}

dependencies {
    implementation(project(":api"))
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
}

tasks {
    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.common.util"
    }
}