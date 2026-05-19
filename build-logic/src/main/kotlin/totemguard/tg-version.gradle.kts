package totemguard

import totemguard.build.TGVersionTask

interface TGVersionPluginExtension {
    val packageName: org.gradle.api.provider.Property<String>
    val className: org.gradle.api.provider.Property<String>
}

val tgVersion = extensions.create<TGVersionPluginExtension>("tgVersion")
tgVersion.packageName.convention("com.deathmotion.totemguard.common.util")
tgVersion.className.convention("TGVersions")

pluginManager.withPlugin("java") {
    val generateTask = tasks.register<TGVersionTask>(TGVersionTask.TASK_NAME) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Generates a TGVersions-style class from the project version."

        packageName.set(tgVersion.packageName)
        className.set(tgVersion.className)
        versionValue.set(project.version.toString())
        outputDir.set(layout.buildDirectory.dir("generated/sources/tgversion/main"))
    }

    val sourceSets = extensions.getByType<SourceSetContainer>()
    sourceSets.named("main") {
        java.srcDir(generateTask.flatMap { it.outputDir })
    }

    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME) {
        dependsOn(generateTask)
    }
}
