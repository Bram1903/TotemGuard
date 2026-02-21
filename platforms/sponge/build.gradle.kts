import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    id("totemguard.java-conventions")
    id("totemguard.shadow-conventions")
    alias(libs.plugins.spongeGradle)
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.sponge)
}

sponge {
    apiVersion("14.0.0")
    license("GPL3")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("totemguard") {
        displayName("TotemGuard")
        entrypoint("com.deathmotion.totemguard.sponge.TGSponge")
        description("TODO")
        version(project.version.toString())
        contributor("Bram") {
            description("Author")
        }
        contributor("OutDev") {
            description("Author")
        }
        dependencies {
            dependency("spongeapi") {
                loadOrder(PluginDependency.LoadOrder.AFTER)
                optional(false)
            }
            dependency("packetevents") {
                loadOrder(PluginDependency.LoadOrder.AFTER)
                version("2.11.0")
                optional(false)
            }
        }
    }
}
