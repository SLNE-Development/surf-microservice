package dev.slne.surf.microservice.runtime.microservice.redis

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.runtime.microservice.MicroserviceLauncher
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.StandaloneRedisInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.OffsetDateTime
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

object RedisService {
    private val log = logger()

    private val redisInstance = StandaloneRedisInstance(
        name = "microservice-redis",
        configPath = Path("config")
    )
    lateinit var redisApi: RedisApi
    var currentStatus = MicroserviceRedisStatus(
        running = true,
        startedAt = OffsetDateTime.now()
    )
        private set

    @Synchronized
    fun updateRedisStatus(status: MicroserviceRedisStatus) {
        currentStatus = status

        redisApi.redissonReactive.getMap<String, MicroserviceRedisStatus>("surf-microservice:v1:status")
            .put(MicroserviceLauncher.holderName, status)
            .subscribe(
                {},
                { error ->
                    log.atSevere()
                        .withCause(error)
                        .log("Failed to update microservice status in Redis")
                }
            )
    }

    @Synchronized
    fun recordError(throwable: Throwable) {
        if (!::redisApi.isInitialized || !redisApi.isConnected()) {
            return
        }

        updateRedisStatus(currentStatus.copy(errors = currentStatus.errors + throwable.stackTraceToString()))
    }

    suspend fun connect() = withTimeout(5.seconds) {
        withContext(Dispatchers.IO) {
            runInterruptible {
                redisInstance.create()
                redisApi = RedisApi.create()
                redisApi.freezeAndConnect()
            }
        }

        updateRedisStatus(currentStatus)
    }

    fun disconnect() {
        if (redisApi.isConnected()) {
            updateRedisStatus(currentStatus.copy(running = false))
        }

        redisApi.disconnect()
    }
}

val redisApi get() = RedisService.redisApi