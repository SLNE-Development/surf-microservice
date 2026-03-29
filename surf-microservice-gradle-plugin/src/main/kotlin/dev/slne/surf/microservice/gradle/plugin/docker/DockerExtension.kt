package dev.slne.surf.microservice.gradle.plugin.docker

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class DockerExtension {
    abstract val baseImage: Property<String>
    abstract val port: Property<Int>
    abstract val jvmArgs: ListProperty<String>
    abstract val repository: Property<DockerRepository>
}
