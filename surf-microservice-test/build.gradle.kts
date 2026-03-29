plugins {
    id("dev.slne.surf.microservice") version "+"
}

surfMicroserviceDocker {
    baseImage.set("ghcr.io/graalvm/jdk-community:25")
    port.set(25565)
    jvmArgs.set(listOf("-Xmx512m", "-Xms256m"))
    repository.set(dev.slne.surf.microservice.gradle.plugin.docker.DockerRepository.PUBLIC)
}
