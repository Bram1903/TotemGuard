package totemguard.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.shadow.conventions")
}

// JDA and its transitives are relocated under a dedicated prefix so this optional jar never
// clashes with the core plugin's shaded libraries. minimize() is intentionally NOT used here:
// JDA and Jackson load many classes reflectively, and tree-shaking would strip them.
tasks.withType<ShadowJar>().configureEach {
    val libsPrefix = "com.deathmotion.totemguard.discord.libs"
    relocate("net.dv8tion.jda", "$libsPrefix.jda")
    relocate("okhttp3", "$libsPrefix.okhttp3")
    relocate("okio", "$libsPrefix.okio")
    relocate("kotlin", "$libsPrefix.kotlin")
    relocate("com.neovisionaries.ws.client", "$libsPrefix.nv")
    relocate("org.apache.commons.collections4", "$libsPrefix.commonscollections4")
    relocate("gnu.trove", "$libsPrefix.trove")
    relocate("com.fasterxml.jackson", "$libsPrefix.jackson")
}
