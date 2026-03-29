package dev.slne.surf.microservice.gradle.plugin.task.docker

import org.gradle.api.tasks.TaskAction

abstract class GeneratePterodactylDockerfileTask : AbstractGenerateDockerfileTask() {

    init {
        description = "Generates a Dockerfile and entrypoint.sh for running the microservice on Pterodactyl"
    }

    override val workdir = "/home/container"
    override val jarName = "app.jar"

    override fun buildDockerfileContent() = buildString {
        appendLine("FROM ${baseImage.get()}")
        appendLine()
        appendLine("RUN adduser --disabled-password --home /home/container container")
        appendLine()
        appendLine("USER container")
        appendLine("ENV USER=container HOME=/home/container")
        appendLine()
        appendLine("WORKDIR $workdir")
        appendLine()
        appendLine("COPY build/libs/*-all.jar $jarName")
        appendLine()
        appendLine("COPY ./entrypoint.sh /entrypoint.sh")
        appendLine()
        appendLine("""CMD ["/bin/bash", "/entrypoint.sh"]""")
        appendLine()
    }

    private fun buildEntrypointContent() = buildString {
        appendLine("#!/bin/bash")
        appendLine()
        appendLine("cd /home/container")
        appendLine()
        appendLine("MODIFIED_STARTUP=\$(eval echo \"\$(echo \${STARTUP} | sed -e 's/{{/\${/g' -e 's/}}/}/g')\")")
        appendLine("echo \":/home/container\$ \${MODIFIED_STARTUP}\"")
        appendLine("\${MODIFIED_STARTUP}")
        appendLine()
    }

    @TaskAction
    fun generateEntrypoint() {
        val entrypointFile = outputFile.get().asFile.parentFile.resolve("entrypoint.sh")
        entrypointFile.writeText(buildEntrypointContent())
        logger.lifecycle("Generated entrypoint.sh at ${entrypointFile.absolutePath}")
    }
}
