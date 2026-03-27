import version.TGVersionTask

pluginManager.withPlugin("java") {
    val generateTask = tasks.register<TGVersionTask>(TGVersionTask.TASK_NAME) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Generates TGVersions.java from the project version."

        packageName.convention("com.deathmotion.totemguard.common.util")
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
