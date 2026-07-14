package totemguard.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.shadow.conventions")
}

tasks.withType<ShadowJar>().configureEach {
    val libsPrefix = "com.deathmotion.totemguard.proxybridge.libs"
    relocate("io.lettuce", "$libsPrefix.lettuce")
    relocate("io.netty", "$libsPrefix.netty")
    relocate("org.reactivestreams", "$libsPrefix.reactivestreams")
    relocate("reactor", "$libsPrefix.reactor")

    minimize()
}
