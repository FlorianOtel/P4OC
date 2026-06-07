package dev.blazelight.p4oc.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dev.blazelight.p4oc.core.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet6Address
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val TAG = "MdnsDiscovery"
private const val SERVICE_TYPE = "_http._tcp."
private const val SERVICE_NAME_PREFIX = "opencode-"
private const val SEED_PROBE_TIMEOUT_SECONDS = 2L
private const val SEED_PROBE_CONCURRENCY = 4

enum class DiscoverySource {
    MDNS,
    SEED,
}

/**
 * A discovered OpenCode server on the local network.
 */
data class DiscoveredServer(
    val serviceName: String,
    val host: String,
    val port: Int,
    val url: String,
    val source: DiscoverySource = DiscoverySource.MDNS,
    val allowInsecure: Boolean = false,
)

data class DiscoverySeed(
    val rawUrl: String,
    val allowInsecure: Boolean = false,
)

internal data class NormalizedSeed(
    val canonicalUrl: String,
    val host: String,
    val port: Int,
    val scheme: String,
    val allowInsecure: Boolean,
)

internal fun normalizeSeed(seed: DiscoverySeed): NormalizedSeed? {
    val canonicalUrl = normalizeSeedUrl(seed.rawUrl) ?: return null
    val parsed = canonicalUrl.toHttpUrlOrNull() ?: return null
    val host = if (':' in parsed.host) "[${parsed.host}]" else parsed.host

    return NormalizedSeed(
        canonicalUrl = canonicalUrl,
        host = host,
        port = parsed.port,
        scheme = parsed.scheme,
        allowInsecure = seed.allowInsecure,
    )
}

private fun normalizeSeedUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null

    val candidate = if (trimmed.contains("://")) trimmed else "http://$trimmed"
    val parsed = candidate.toHttpUrlOrNull() ?: return null
    if (parsed.scheme != "http" && parsed.scheme != "https") return null

    val builder = parsed.newBuilder()
        .query(null)
        .fragment(null)

    if (!hasExplicitSeedPort(candidate)) {
        builder.port(ServerUrl.DEFAULT_PORT)
    }

    val normalized = builder.build()
    val path = normalized.encodedPath.takeUnless { it == "/" }?.trimEnd('/').orEmpty()
    return buildString {
        append(normalized.scheme)
        append("://")
        append(formatSeedHost(normalized.host))
        append(':')
        append(normalized.port)
        append(path)
    }
}

private fun formatSeedHost(host: String): String = if (':' in host) "[$host]" else host

private fun hasExplicitSeedPort(candidate: String): Boolean {
    val authority = candidate
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')

    if (authority.startsWith("[")) {
        return authority.substringAfter("]", missingDelimiterValue = "").startsWith(":")
    }

    return authority.contains(':')
}

internal fun endpointKey(url: String): String = ServerUrl.endpointKey(url) ?: url.trim()

internal fun mergeDiscoveredServer(
    existing: List<DiscoveredServer>,
    incoming: DiscoveredServer,
): List<DiscoveredServer> {
    val existingIndex = existing.indexOfFirst { endpointKey(it.url) == endpointKey(incoming.url) }
    if (existingIndex < 0) return existing + incoming

    val current = existing[existingIndex]
    val replacement = when {
        current.source == DiscoverySource.SEED && incoming.source == DiscoverySource.MDNS -> incoming.copy(
            allowInsecure = current.allowInsecure || incoming.allowInsecure,
        )
        current.source == DiscoverySource.MDNS && incoming.source == DiscoverySource.SEED -> current.copy(
            allowInsecure = current.allowInsecure || incoming.allowInsecure,
        )
        else -> incoming.copy(
            allowInsecure = current.allowInsecure || incoming.allowInsecure,
        )
    }

    return existing.toMutableList().apply { this[existingIndex] = replacement }
}

/**
 * State of mDNS discovery scanning.
 */
enum class DiscoveryState {
    IDLE,
    SCANNING,
    ERROR
}

/**
 * Manages mDNS/NSD discovery of OpenCode servers on the local network.
 *
 * Uses Android's [NsdManager] to browse for `_http._tcp.` services whose name
 * starts with `opencode-`. Resolves each discovered service sequentially
 * (NSD limitation: only one resolve at a time) and exposes results via
 * [discoveredServers] and [discoveryState] StateFlows.
 */
class MdnsDiscoveryManager(private val context: Context) {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var seedProbeJob: Job? = null
    private var resolveParentJob: Job? = null

    private val strictProbeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val insecureProbeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(SEED_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .applyInsecureTls()
            .build()
    }

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _discoveryState = MutableStateFlow(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    /** NSD can only resolve one service at a time. */
    private val resolveMutex = Mutex()

    private var activeListener: NsdManager.DiscoveryListener? = null

    /**
     * Start browsing for OpenCode servers on the local network.
     * If already scanning, stops the previous scan first.
     */
    fun startDiscovery() = startDiscovery(emptyList())

    fun startDiscovery(seeds: List<DiscoverySeed>) {
        if (activeListener != null || seedProbeJob != null) {
            AppLog.d(TAG, "Stopping previous discovery before restarting")
            stopDiscovery()
        }

        _discoveredServers.value = emptyList()
        _discoveryState.value = DiscoveryState.SCANNING
        resolveParentJob = SupervisorJob()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                AppLog.d(TAG, "Discovery started for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName
                AppLog.d(TAG, "Service found: $name")
                if (name.startsWith(SERVICE_NAME_PREFIX, ignoreCase = true)) {
                    AppLog.d(TAG, "OpenCode service matched: $name, queuing resolve")
                    launchResolve(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName
                AppLog.d(TAG, "Service lost: $name")
                _discoveredServers.update { servers ->
                    servers.filter { it.serviceName != name }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                AppLog.d(TAG, "Discovery stopped for $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLog.e(TAG, "Start discovery failed: errorCode=$errorCode")
                activeListener = null
                _discoveryState.value = DiscoveryState.ERROR
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                AppLog.w(TAG, "Stop discovery failed: errorCode=$errorCode")
            }
        }

        activeListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            startSeedProbing(seeds)
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to start discovery", e)
            activeListener = null
            resolveParentJob?.cancel()
            resolveParentJob = null
            _discoveryState.value = DiscoveryState.ERROR
        }
    }

    /**
     * Stop browsing for servers.
     */
    fun stopDiscovery() {
        seedProbeJob?.cancel()
        seedProbeJob = null
        resolveParentJob?.cancel()
        resolveParentJob = null

        val listener = activeListener
        activeListener = null

        if (listener != null) {
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                AppLog.w(TAG, "Error stopping discovery: ${e.message}")
            }
        }

        _discoveryState.value = DiscoveryState.IDLE
    }

    private fun startSeedProbing(seeds: List<DiscoverySeed>) {
        val normalizedSeeds = seeds.mapNotNull(::normalizeSeed).distinctBy { it.canonicalUrl }
        if (normalizedSeeds.isEmpty()) return

        seedProbeJob = scope.launch {
            val semaphore = Semaphore(SEED_PROBE_CONCURRENCY)
            coroutineScope {
                normalizedSeeds.map { seed ->
                    launch {
                        semaphore.withPermit {
                            probeSeed(seed)
                        }
                    }
                }.joinAll()
            }
        }
    }

    private fun probeSeed(seed: NormalizedSeed) {
        val client = if (seed.allowInsecure) insecureProbeClient else strictProbeClient
        val request = Request.Builder()
            .url("${seed.canonicalUrl}/project")
            .get()
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    val server = DiscoveredServer(
                        serviceName = "seed:${seed.host}:${seed.port}",
                        host = seed.host,
                        port = seed.port,
                        url = seed.canonicalUrl,
                        source = DiscoverySource.SEED,
                        allowInsecure = seed.allowInsecure,
                    )
                    AppLog.d(TAG, "Seed probe passed for ${seed.canonicalUrl}")
                    _discoveredServers.update { servers -> mergeDiscoveredServer(servers, server) }
                } else {
                    AppLog.d(TAG, "Seed probe failed for ${seed.canonicalUrl}: HTTP ${response.code}")
                }
            }
        }.onFailure { error ->
            AppLog.d(TAG, "Seed probe failed for ${seed.canonicalUrl}: ${error.message}")
        }
    }

    private fun launchResolve(serviceInfo: NsdServiceInfo) {
        val parent = resolveParentJob ?: return
        scope.launch(parent) {
            resolveMutex.withLock {
                val resolvedInfo = resolveService(serviceInfo) ?: return@withLock
                val server = resolvedInfo.toDiscoveredServer() ?: return@withLock

                AppLog.d(TAG, "Resolved: ${server.serviceName} -> ${server.url}")

                _discoveredServers.update { servers ->
                    mergeDiscoveredServer(servers, server)
                }
            }
        }
    }

    private suspend fun resolveService(serviceInfo: NsdServiceInfo): NsdServiceInfo? {
        return suspendCancellableCoroutine { continuation ->
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    AppLog.w(TAG, "Resolve failed for ${info.serviceName}: errorCode=$errorCode")
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    if (continuation.isActive) continuation.resume(info)
                }
            }

            try {
                nsdManager.resolveService(serviceInfo, listener)
            } catch (e: Exception) {
                AppLog.e(TAG, "Error resolving service: ${e.message}", e)
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private fun NsdServiceInfo.toDiscoveredServer(): DiscoveredServer? {
        val host = host
        val port = port
        val hostAddress = host?.hostAddress
        if (hostAddress == null) {
            AppLog.w(TAG, "Resolved $serviceName but hostAddress is null, skipping")
            return null
        }

        val formattedHost = if (host is Inet6Address) {
            "[${hostAddress.split("%").first()}]"
        } else {
            hostAddress
        }

        val url = ServerUrl.normalizeConnectUrl("http://$formattedHost:$port") ?: "http://$formattedHost:$port"
        return DiscoveredServer(
            serviceName = serviceName,
            host = formattedHost,
            port = port,
            url = url,
            source = DiscoverySource.MDNS,
            allowInsecure = false,
        )
    }
}
