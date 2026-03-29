package dev.slne.surf.microservice.gradle.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateDockerfileTask : DefaultTask() {
    @get:Input
    abstract val baseImage: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "microservice"
        description = "Generates a Dockerfile for the microservice"
    }

    @TaskAction
    fun generate() {
        val image = baseImage.get()
        val exposedPort = port.get()
        val args = jvmArgs.get()

        val entrypoint = if (args.isEmpty()) {
            """ENTRYPOINT ["java", "-jar", "app.jar"]"""
        } else {
            val jvmArgsStr = args.joinToString(", ") { "\"$it\"" }
            """ENTRYPOINT ["java", $jvmArgsStr, "-jar", "app.jar"]"""
        }

        val dockerfile = buildString {
            appendLine("FROM $image")
            appendLine()
            appendLine("WORKDIR /app")
            appendLine()
            appendLine("COPY build/libs/*-all.jar app.jar")
            appendLine()
            appendLine("EXPOSE $exposedPort")
            appendLine()
            appendLine(entrypoint)
            appendLine()
        }

        outputFile.get().asFile.writeText(dockerfile)
        logger.lifecycle("Generated Dockerfile at ${outputFile.get().asFile.absolutePath}")
    }
}
