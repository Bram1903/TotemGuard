plugins {
    id("totemguard.java.internal")
}

description = "Platform-agnostic Discord bot core for TotemGuard (JDA, slash commands, alert relay)."

dependencies {
    // The TotemGuard API is provided at runtime by the running plugin (shared classloader),
    // so it must never be shaded into the bot jar.
    compileOnly(projects.api)

    implementation(libs.jda) {
        exclude(group = "club.minnced", module = "opus-java")        // voice encode natives
        exclude(group = "com.google.crypto.tink", module = "tink")   // voice (DAVE) encryption
        exclude(group = "org.slf4j")                                  // provided by Paper/Fabric
    }

    // SnakeYAML and SLF4J are provided by the host platform at runtime (Paper ships both;
    // the Fabric bootstrap bundles SnakeYAML). Compile against them only.
    compileOnly(libs.snakeyaml)
    compileOnly(libs.slf4j.api)
}
