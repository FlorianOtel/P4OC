package dev.blazelight.p4oc.domain.server

import dev.blazelight.p4oc.core.network.ServerUrl

@JvmInline
value class ServerGeneration(val value: Long) {
    init {
        require(value >= 0L) { "Server generation must be non-negative" }
    }
}

class ServerRef private constructor(
    val endpointKey: String,
    val displayName: String,
) {
    val badgeLabel: String
        get() = ServerIdentity.derive(endpointKey, displayName).badgeLabel
    override fun equals(other: Any?): Boolean =
        this === other || (other is ServerRef && endpointKey == other.endpointKey)

    override fun hashCode(): Int = endpointKey.hashCode()

    override fun toString(): String = "ServerRef(endpointKey=$endpointKey, displayName=$displayName)"

    companion object {
        fun fromEndpoint(input: String, displayName: String? = null): ServerRef {
            val identity = ServerIdentity.derive(input, displayName)
            val key = ServerUrl.endpointKey(input)
                ?: throw IllegalArgumentException("Invalid server endpoint: $input")
            return ServerRef(
                endpointKey = key,
                displayName = identity.displayName,
            )
        }

        fun fromEndpointKey(endpointKey: String, displayName: String? = null): ServerRef {
            require(endpointKey.isNotBlank()) { "Endpoint key must not be blank" }
            val identity = ServerIdentity.derive(endpointKey, displayName)
            return ServerRef(
                endpointKey = endpointKey,
                displayName = identity.displayName,
            )
        }
    }
}
