package dev.slne.surf.microservice.gradle.plugin.docker

enum class DockerRepository(
    val registryUrl: String,
    val usernameSecret: String,
    val passwordSecret: String
) {
    PUBLIC(
        "repo.slne.dev/slne-docker-public",
        "SLNE_RELEASES_REPO_USERNAME",
        "SLNE_RELEASES_REPO_PASSWORD"
    ),
    PRIVATE(
        "repo.slne.dev/slne-docker-private",
        "SLNE_SNAPSHOTS_REPO_USERNAME",
        "SLNE_SNAPSHOTS_REPO_PASSWORD"
    )
}
