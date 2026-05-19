package totemguard.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.shadow.conventions")
}

tasks.withType<ShadowJar>().configureEach {
    val libsPrefix = "com.deathmotion.totemguard.common.libs"
    relocate("io.lettuce", "$libsPrefix.lettuce")
    relocate("io.netty", "$libsPrefix.netty")
    relocate("org.reactivestreams", "$libsPrefix.reactivestreams")
    relocate("reactor", "$libsPrefix.reactor")
    relocate("redis.clients", "$libsPrefix.redisclients")
    relocate("com.zaxxer.hikari", "$libsPrefix.hikari")
    relocate("com.google.errorprone.annotations", "$libsPrefix.errorprone.annotations")
    relocate("org.jspecify.annotations", "$libsPrefix.jspecify.annotations")
    relocate("org.bstats", "$libsPrefix.bstats")

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    minimize {
        exclude(dependency("org.bstats:.*:.*"))
        exclude(dependency("com.mysql:.*"))
        exclude(project(":loader:host"))
    }
}
