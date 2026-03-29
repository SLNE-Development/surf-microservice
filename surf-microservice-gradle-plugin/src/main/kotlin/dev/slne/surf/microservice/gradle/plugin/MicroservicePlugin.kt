package dev.slne.surf.microservice.gradle.plugin

import dev.slne.surf.microservice.gradle.generated.Constants
import dev.slne.surf.microservice.gradle.plugin.task.GenerateDockerfileTask
import dev.slne.surf.microservice.gradle.plugin.task.GenerateWorkflowTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

abstract class MicroservicePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<MicroserviceExtension>("surfMicroservice")

        tasks.register<GenerateDockerfileTask>("generateDockerfile") {
            baseImage.convention("ghcr.io/graalvm/jdk-community:25")
            port.convention(8080)
            jvmArgs.convention(emptyList())
            outputFile.convention(layout.projectDirectory.file("Dockerfile"))
        }

        tasks.register<GenerateWorkflowTask>("generateWorkflow") {
            moduleRegex.convention("")
            outputFile.convention(layout.projectDirectory.file(".github/workflows/publish.yml"))
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