plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

@Suppress("UnstableApiUsage")
val versionCatalogAccessorsClasspath = files(libs.javaClass.superclass.protectionDomain.codeSource.location)

dependencies {
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.run.paper.gradle.plugin)
    implementation(libs.run.velocity.gradle.plugin)
    implementation(versionCatalogAccessorsClasspath)
}
