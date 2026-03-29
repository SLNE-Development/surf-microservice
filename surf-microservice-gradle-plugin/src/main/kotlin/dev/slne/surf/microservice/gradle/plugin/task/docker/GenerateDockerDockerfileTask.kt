package dev.slne.surf.microservice.gradle.plugin.task.docker

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class GenerateDockerDockerfileTask : AbstractGenerateDockerfileTask() {
    @get:Input
    abstract val port: Property<Int>

    init {
        description = "Generates a Dockerfile for running the microservice in Docker/Kubernetes"
    }

    override val workdir = "/app"
    override val jarName = "app.jar"

    override fun buildDockerfileContent() = buildString {
        appendLine("FROM ${baseImage.get()}")
        appendLine()
        appendLine("WORKDIR $workdir")
        appendLine()
        appendLine("COPY build/libs/*-all.jar $jarName")
        appendLine()
        appendLine("EXPOSE ${port.get()}")
        appendLine()
        appendLine(buildEntrypoint())
        appendLine()
    }
}
