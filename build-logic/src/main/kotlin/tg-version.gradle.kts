import version.TGVersionTask

interface TGVersionPluginExtension {
    val packageName: org.gradle.api.provider.Property<String>
    val className: org.gradle.api.provider.Property<String>
}

val ext = extensions.create<TGVersionPluginExtension>("tgVersion")
ext.packageName.convention("com.deathmotion.totemguard.common.util")
ext.className.convention("TGVersions")

pluginManager.withPlugin("java") {
    val generateTask = tasks.register<TGVersionTask>(TGVersionTask.TASK_NAME) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Generates a TGVersions-style class from the project version."

        packageName.set(ext.packageName)
        className.set(ext.className)
        versionValue.set(provider { project.version.toString() })
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
