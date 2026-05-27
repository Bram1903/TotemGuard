package totemguard

import totemguard.build.BuildPropertiesTask
import totemguard.build.TGVersionTask

interface TGVersionPluginExtension {
    val packageName: Property<String>
    val className: Property<String>
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

    val rootGitHash = rootProject.extra.properties["gitHash"] as String?
    val buildPropsTask = tasks.register<BuildPropertiesTask>(BuildPropertiesTask.TASK_NAME) {
        group = LifecycleBasePlugin.BUILD_GROUP
        description = "Generates META-INF/totemguard/build.properties for runtime identification."
        versionValue.set(project.version.toString())
        if (rootGitHash != null) gitCommit.set(rootGitHash)
        outputDir.set(layout.buildDirectory.dir("generated/resources/build-properties"))
    }

    sourceSets.named("main") {
        resources.srcDir(buildPropsTask.flatMap { it.outputDir })
    }
}
