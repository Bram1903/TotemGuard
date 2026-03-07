plugins {
    `java-library`
    id("totemguard.java-conventions")
    id("tg-version")
}

dependencies {
    api(project(":api"))
    implementation(libs.lettuce) {
        exclude(group = "org.slf4j")
    }
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
    compileOnly(libs.bundles.adventure.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.snakeyaml)
    compileOnly(libs.cloud.core)
}
