package dev.slne.surf.microservice.api.microservice

import dev.slne.surf.microservice.api.util.InternalMicroserviceApi
import java.nio.file.Path

abstract class Microservice {
    lateinit var dataPath: Path
        private set

    @InternalMicroserviceApi
    suspend fun bootstrap(args: List<String>, dataPath: Path) {
        this.dataPath = dataPath

        onBootstrap(args)
    }

    @InternalMicroserviceApi
    suspend fun disable() {
        onDisable()
    }

    open suspend fun onBootstrap(args: List<String>) {}
    open suspend fun onDisable() {}

    companion object {
        @InternalMicroserviceApi
        lateinit var INSTANCE: Microservice
    }
}

val microservice get() = Microservice.INSTANCE