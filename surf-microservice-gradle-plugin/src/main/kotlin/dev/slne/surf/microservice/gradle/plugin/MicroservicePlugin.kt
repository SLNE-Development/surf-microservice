package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.plugin.docker.DockerExtension
import dev.slne.surf.microservice.gradle.plugin.docker.DockerRepository
import dev.slne.surf.microservice.gradle.plugin.task.docker.GenerateDockerDockerfileTask
import dev.slne.surf.microservice.gradle.plugin.task.docker.GeneratePterodactylDockerfileTask
import dev.slne.surf.microservice.gradle.plugin.task.workflow.GenerateBuildWorkflowTask
import dev.slne.surf.microservice.gradle.plugin.task.workflow.GenerateDockerWorkflowTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")
        val dockerExtension = extensions.create<DockerExtension>("surfMicroserviceDocker")

        dockerExtension.baseImage.convention("ghcr.io/graalvm/jdk-community:25")
        dockerExtension.port.convention(8080)
        dockerExtension.jvmArgs.convention(emptyList())
        dockerExtension.repository.convention(DockerRepository.PRIVATE)

        tasks.register<GenerateDockerDockerfileTask>("generateDockerDockerfile") {
            baseImage.convention(dockerExtension.baseImage)
            port.convention(dockerExtension.port)
            jvmArgs.convention(dockerExtension.jvmArgs)
            outputFile.convention(layout.projectDirectory.file("Dockerfile"))
        }

        tasks.register<GeneratePterodactylDockerfileTask>("generatePterodactylDockerfile") {
            baseImage.convention(dockerExtension.baseImage)
            jvmArgs.convention(dockerExtension.jvmArgs)
            outputFile.convention(layout.projectDirectory.file("Dockerfile"))
        }

        tasks.register<GenerateBuildWorkflowTask>("generateBuildWorkflow") {
            moduleRegex.convention("")
            outputFile.convention(layout.projectDirectory.file(".github/workflows/build.yml"))
        }

        tasks.register<GenerateDockerWorkflowTask>("generateDockerWorkflow") {
            registryUrl.convention(dockerExtension.repository.map { it.registryUrl })
            usernameSecret.convention(dockerExtension.repository.map { it.usernameSecret })
            passwordSecret.convention(dockerExtension.repository.map { it.passwordSecret })
            outputFile.convention(layout.projectDirectory.file(".github/workflows/docker.yml"))
        }

        afterEvaluate {
            extension.module.orNull?.let { moduleDependency ->
                val apiModule = moduleDependency.apiModule
                val runtimeModule = moduleDependency.runtimeModule
                val projectModification = moduleDependency.moduleProjectModification

                dependencies.add(
                    "compileOnlyApi",
                    "dev.slne.surf.microservice:${apiModule}:${Constants.MINECRAFT_VERSION}+"
                )

                dependencies.add(
                    "runtimeOnly",
                    "dev.slne.surf.microservice:${runtimeModule}:${Constants.MINECRAFT_VERSION}+"
                )

                projectModification(this)
            }

            extension.rabbitSettings.orNull?.let { rabbitSettings ->
                val moduleName = rabbitSettings.rabbitModule.module
                val applyServerRuntimeDependency = rabbitSettings.applyRabbitServerRuntimeDependency

                dependencies.add(
                    "compileOnlyApi",
                    "dev.slne.surf.rabbitmq:surf-rabbitmq-$moduleName:+"
                )

                if (applyServerRuntimeDependency) {
                    dependencies.add(
                        "runtimeOnly",
                        "dev.slne.surf.rabbitmq:surf-rabbitmq-server:+"
                    )
                }
            }
        }
    }
}
