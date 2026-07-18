package dev.slne.surf.microservice.runtime.microservice

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi
import dev.slne.surf.microservice.api.microservice.Microservice
import dev.slne.surf.microservice.runtime.microservice.command.microserviceCommandManagerImpl
import dev.slne.surf.microservice.runtime.microservice.redis.RedisService
import dev.slne.surf.microservice.runtime.microservice.spark.MicroserviceSpark
import kotlinx.coroutines.*
import java.util.*

private val log = logger()

private val useRedis by lazy {
    System.getProperty("surf.microservice.redis", "true").toBoolean()
}

object MicroserviceLauncher {
    private val scope =
        CoroutineScope(Dispatchers.Default + CoroutineName("MicroserviceLauncher") + SupervisorJob() + CoroutineExceptionHandler { context, exception ->
            log.atSevere()
                .withCause(exception)
                .log("An uncaught exception occurred in the microservice scope on ${context[CoroutineName]}")

            RedisService.recordError(exception)
        })

    val holderName by lazy { microservice.holderName }

    suspend fun launch(args: Array<String>) {
        microserviceCommandManagerImpl.initDefaultCommands()

        SurfApiStandaloneBootstrap.bootstrap()
        SurfApiStandaloneBootstrap.enable()

        microservice.bootstrap(args.toList())

        scope.launch(Dispatchers.IO + CoroutineName("MicroserviceCommandListener")) {
            val scanner = Scanner(System.`in`)

            while (isActive && scanner.hasNextLine()) {
                val line = scanner.nextLine()

                microserviceCommandManagerImpl.handleCommand(line, scope)
            }
        }

        if (useRedis) {
            log.atInfo().log("Connecting to Redis...")
            RedisService.connect()
        } else {
            log.atInfo().log("Skipping Redis connection...")
        }

        log.atInfo().log("Microservice started successfully. Type 'exit' or 'quit' to shut down.")

        awaitCancellation()
    }

    @InternalMicroserviceApi
    lateinit var microservice: Microservice
        internal set
}

suspend fun main(args: Array<String>) {
    MicroserviceLauncher.microservice = requiredService<Microservice>()
    Microservice.INSTANCE = MicroserviceLauncher.microservice

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            MicroserviceLauncher.microservice.disable()
            MicroserviceSpark.onDisable()

            if (useRedis) {
                RedisService.disconnect()
            }

            SurfApiStandaloneBootstrap.shutdown()
        }
    })

    MicroserviceSpark.onLoad()
    MicroserviceLauncher.launch(args)
}