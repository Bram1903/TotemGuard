plugins {
    `java-library`
    id("totemguard.java.internal")
    id("totemguard.tg-version")
}

dependencies {
    api(projects.api)
    api(projects.loader.host)
    implementation(projects.integrity)
    implementation(projects.bridge.protocol)
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
    compileOnly(libs.luckperms.api)
    compileOnly(libs.viaversion.api)
}
