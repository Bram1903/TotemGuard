package totemguard.runs

import org.gradle.accessors.dm.LibrariesForLibs
import xyz.jpenilla.runvelocity.task.RunVelocity

plugins {
    id("xyz.jpenilla.run-velocity")
}

val libs = the<LibrariesForLibs>()
val velocityVersionString: String = libs.versions.velocity.get()

tasks.withType<RunVelocity>().configureEach {
    velocityVersion(velocityVersionString)
    runDirectory.set(rootProject.layout.projectDirectory.dir("run/velocity"))
}
