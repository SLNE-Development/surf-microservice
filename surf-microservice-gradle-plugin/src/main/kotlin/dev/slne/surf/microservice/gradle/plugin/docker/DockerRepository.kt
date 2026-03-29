package dev.slne.surf.microservice.gradle.plugin.docker

enum class DockerRepository(val registryUrl: String) {
    PUBLIC("repo.slne.dev/slne-docker-public"),
    PRIVATE("repo.slne.dev/slne-docker-private")
}
