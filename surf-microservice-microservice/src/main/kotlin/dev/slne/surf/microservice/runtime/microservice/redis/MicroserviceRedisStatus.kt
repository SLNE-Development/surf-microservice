package dev.slne.surf.microservice.runtime.microservice.redis

import dev.slne.surf.api.core.serializer.java.datetime.datetime.offset.SerializableOffsetDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MicroserviceRedisStatus(
    val running: Boolean,
    val startedAt: SerializableOffsetDateTime,
    val errors: List<String> = mutableListOf(),
)