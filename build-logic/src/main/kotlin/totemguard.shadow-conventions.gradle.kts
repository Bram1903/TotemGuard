import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import java.util.*

plugins {
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<ShadowJar>().configureEach {
    archiveFileName =
        "${rootProject.name}-${project.name.capitalizedName()}-${project.version.toString().withoutSnapshotHash()}.jar"
    archiveClassifier = null
    destinationDirectory = rootProject.layout.buildDirectory
    exclude("META-INF/maven/**")

    relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
    relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")
    relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
    relocate("io.lettuce", "com.deathmotion.totemguard.common.libs.lettuce")
    relocate("io.netty", "com.deathmotion.totemguard.common.libs.netty")
    relocate("org.reactivestreams", "com.deathmotion.totemguard.common.libs.reactivestreams")
    relocate("reactor", "com.deathmotion.totemguard.common.libs.reactor")
    relocate("redis.clients", "com.deathmotion.totemguard.common.libs.redisclients")
    relocate("com.google.errorprone.annotations", "com.deathmotion.totemguard.common.libs.errorprone.annotations")
    relocate("org.jspecify.annotations", "com.deathmotion.totemguard.common.libs.jspecify.annotations")

    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

fun String.withoutSnapshotHash(): String {
    return replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
}

fun String.capitalizedName(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
