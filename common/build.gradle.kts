plugins {
    `java-library`
    id("totemguard.java-conventions")
    id("tg-version")
}

dependencies {
    api(project(":api"))
    api(project(":loader:host"))
    implementation(project(":integrity"))
    implementation(project(":bridge:protocol"))
    implementation(libs.lettuce) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.hikaricp) {
        exclude(group = "org.slf4j")
    }
    compileOnly(libs.mysql.jdbc)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.adventure.serializers)
    compileOnly(libs.bundles.adventure.minimessage)
    compileOnly(libs.guava)
    compileOnly(libs.snakeyaml)
    compileOnly(libs.cloud.core)
    compileOnly(libs.grim.api)
}
