import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java.internal")
    id("totemguard.shadow.plugin")
    id("totemguard.runs.paper")
    id("totemguard.manifest-expand")
}

dependencies {
    implementation(projects.common)
    implementation(libs.bstats.base)
    implementation(libs.cloud.paper)
    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.placeholderapi)
}

tasks.withType<ShadowJar>().configureEach {
    val libsPrefix = "com.deathmotion.totemguard.common.libs"
    relocate("org.incendo.cloud", "$libsPrefix.cloud")
    relocate("io.leangen.geantyref", "$libsPrefix.geantyref")
}

paperRuns {
    default("1.21.11", java = 21)
    version("1.19.4", java = 17)
    version("1.20.4", java = 17)
    version("1.21.1", java = 21)
    version("1.21.2", java = 21)
    version("1.21.4", java = 21)
    version("26.1.2", java = 25)

    defaultFolia("1.21.11", java = 21)
    folia("1.19.4", java = 17)
    folia("1.20.4", java = 17)
    folia("1.21.1", java = 21)
    folia("1.21.2", java = 21)
    folia("1.21.4", java = 21)
    folia("26.1.2", java = 25)
}
