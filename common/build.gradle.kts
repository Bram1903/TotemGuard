plugins {
    `java-library`
    totemguard.`java-conventions`
    `tg-version`
}

dependencies {
    api(project(":api"))
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
    compileOnly(libs.bundles.adventure.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.cloud.core)
    implementation(libs.configurate.yaml)
}

tasks {
    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.common.util"
    }
}