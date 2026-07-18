package dev.slne.surf.microservice.test

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.random
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import java.nio.file.Path
import kotlin.io.path.Path

@AutoService(Microservice::class)
class TestMicroservice : Microservice("surf-test") {
    override val dataPath = Path("config")

    override suspend fun onBootstrap(args: List<String>) {
        microserviceCommand("triggererror") {
            throw RuntimeException("Test error triggered: ${random.nextInt(2)}")
        }
    }
}