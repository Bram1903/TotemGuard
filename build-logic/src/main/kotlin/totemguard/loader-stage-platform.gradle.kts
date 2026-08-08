package totemguard

import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    java
}

abstract class StageLoaderJarExtension {
    abstract val sourceShadowJar: RegularFileProperty
    abstract val destinationDir: DirectoryProperty
    abstract val attachToTask: Property<String>
}

val stageLoaderJar = extensions.create<StageLoaderJarExtension>("stageLoaderJar")

afterEvaluate {
    val attachTo = stageLoaderJar.attachToTask.orNull ?: return@afterEvaluate
    val source = stageLoaderJar.sourceShadowJar
    val destination = stageLoaderJar.destinationDir

    tasks.named(attachTo) {
        inputs.file(source)
        doFirst {
            val target = destination.get().asFile.toPath()
            Files.createDirectories(target)
            Files.newDirectoryStream(target, "*.jar").use { entries ->
                entries.forEach { Files.delete(it) }
            }
            val sourcePath = source.get().asFile.toPath()
            Files.copy(
                sourcePath,
                target.resolve(sourcePath.fileName.toString()),
                StandardCopyOption.REPLACE_EXISTING,
            )
            logger.lifecycle("Staged ${sourcePath.fileName} -> $target for /tgloader LOCAL source.")
        }
    }
}
