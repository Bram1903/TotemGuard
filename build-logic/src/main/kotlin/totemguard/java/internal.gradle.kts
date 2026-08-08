package totemguard.java

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("totemguard.java.standard")
}

val libs = the<LibrariesForLibs>()

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
