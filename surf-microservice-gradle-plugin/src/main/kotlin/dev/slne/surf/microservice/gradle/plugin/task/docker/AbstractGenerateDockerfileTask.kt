package dev.slne.surf.microservice.gradle.plugin.task.docker

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class AbstractGenerateDockerfileTask : DefaultTask() {
    @get:Input
    abstract val baseImage: Property<String>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "microservice"
    }

    @get:Internal
    protected abstract val workdir: String

    @get:Internal
    protected abstract val jarName: String

    protected abstract fun buildDockerfileContent(): String

    protected fun buildEntrypoint(): String {
        val args = jvmArgs.get()

        return if (args.isEmpty()) {
            """ENTRYPOINT ["java", "-jar", "$jarName"]"""
        } else {
            val jvmArgsStr = args.joinToString(", ") { "\"$it\"" }
            """ENTRYPOINT ["java", $jvmArgsStr, "-jar", "$jarName"]"""
        }
    }

    @TaskAction
    fun generate() {
        val content = buildDockerfileContent()
        outputFile.get().asFile.writeText(content)
        logger.lifecycle("Generated Dockerfile at ${outputFile.get().asFile.absolutePath}")
    }
}
