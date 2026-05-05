package dev.blazelight.p4oc.bench

import dev.blazelight.p4oc.core.network.OpenCodeApi
import dev.blazelight.p4oc.data.remote.dto.CreateSessionRequest
import dev.blazelight.p4oc.data.remote.dto.MessageWrapperDto
import dev.blazelight.p4oc.data.remote.dto.ShellCommandRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Manual OFISH upload chunk benchmark.
 *
 * Run target:
 *   PENCODE_SERVER_PASSWORD=hunter2 opencode serve --hostname 0.0.0.0 --port 4096
 *
 * Suggested invocation after test classes are compiled:
 *   ./gradlew :app:compileDebugUnitTestKotlin
 *   java -cp "app/build/tmp/kotlin-classes/debugUnitTest:app/build/tmp/kotlin-classes/debug:$(find ~/.gradle/caches/modules-2/files-2.1 -name '*.jar' | tr '\n' ':')" \
 *     -DOFISH_BENCH_BASE_URL=http://localhost:4096 \
 *     -DOFISH_BENCH_USERNAME=opencode \
 *     -DOFISH_BENCH_PASSWORD=hunter2 \
 *     dev.blazelight.p4oc.bench.ChunkSizeBenchmark
 *
 * Optional explicit directory:
 *   -DOFISH_BENCH_DIRECTORY=/path/to/workspace
 *
 * If no directory is provided, this passes an explicit null directory to the API.
 */
object ChunkSizeBenchmark {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        run()
    }

    suspend fun run() {
        val config = BenchmarkConfig.fromEnvironment()
        val api = buildApi(config)
        val session = api.createSession(
            directory = config.directory,
            request = CreateSessionRequest(title = "OFISH chunk benchmark"),
        )
        try {
            val sizes = sequence {
                yield(64 * 1024)
                yield(256 * 1024)
                yield(1024 * 1024)
                yield(4 * 1024 * 1024)
                yield(16 * 1024 * 1024)
                var next = 64 * 1024 * 1024
                while (true) {
                    yield(next)
                    next = Math.multiplyExact(next, 2)
                }
            }

            val results = mutableListOf<BenchmarkResult>()
            for (size in sizes) {
                val result = runSize(api, session.id, config.directory, size)
                results += result
                println(result.summary())
                if (!result.success) break
            }

            val best = results
                .filter { it.success }
                .maxByOrNull { it.bytesPerSecond }
                ?: error("No successful chunk-size probe")

            println("const val OFISH_DEFAULT_CHUNK_BYTES: Int = ${best.bytes}")
        } finally {
            runCatching { api.deleteSession(session.id, config.directory) }
        }
    }

    private suspend fun runSize(
        api: OpenCodeApi,
        sessionId: String,
        directory: String?,
        bytes: Int,
    ): BenchmarkResult {
        val payload = deterministicPayload(bytes)
        val expectedHash = payload.sha256Hex()
        val command = buildProbeCommand(bytes, Base64.getEncoder().encodeToString(payload), expectedHash)
        var response: MessageWrapperDto? = null
        val elapsedMs = runCatching {
            measureTimeMillis {
                response = api.executeShellCommand(
                    sessionId = sessionId,
                    request = ShellCommandRequest(
                        agent = "build",
                        model = null,
                        command = command,
                    ),
                    directory = directory,
                )
            }
        }.getOrElse { error ->
            return BenchmarkResult(bytes = bytes, elapsedMs = 0, success = false, message = error.message ?: error::class.java.simpleName)
        }

        val output = response?.extractOutput().orEmpty()
        val success = output.lineSequence().any { it.trim().startsWith("### 200 ok") }
        return BenchmarkResult(
            bytes = bytes,
            elapsedMs = elapsedMs,
            success = success,
            message = output.lineSequence().lastOrNull { it.trim().startsWith("### ") } ?: output.take(200),
        )
    }

    private fun buildProbeCommand(bytes: Int, payloadBase64: String, expectedHash: String): String = buildString {
        val delimiter = "__OFISH_BENCH_${SecureRandom().nextInt().toUInt()}__"
        appendLine("printf '#OFISH_CHUNK_BENCH bytes=$bytes\\n'")
        append(
            """
            TMP=${'$'}(mktemp ".ofish.bench.XXXXXX") || { printf '### 500 failed reason=mktemp\n'; exit 0; }
            cleanup() { rm -f -- "${'$'}TMP" >/dev/null 2>&1 || true; }
            trap cleanup EXIT INT TERM
            base64 -d > "${'$'}TMP" <<'$delimiter'
            """.trimIndent(),
        )
        append('\n')
        append(payloadBase64)
        append('\n')
        append(delimiter)
        append('\n')
        append(
            """
            if [ ${'$'}? -ne 0 ]; then printf '### 500 failed reason=decode\n'; exit 0; fi
            HASH=${'$'}(sha256sum "${'$'}TMP" 2>/dev/null | awk '{print ${'$'}1}')
            if [ "${'$'}HASH" != '$expectedHash' ]; then
              printf '### 500 failed reason=hash actual=%s\n' "${'$'}HASH"
              exit 0
            fi
            rm -f -- "${'$'}TMP" >/dev/null 2>&1 || { printf '### 500 failed reason=rm\n'; exit 0; }
            trap - EXIT INT TERM
            printf '### 200 ok bytes=$bytes hash=%s\n' "${'$'}HASH"
            exit 0
            """.trimIndent(),
        )
    }

    private fun buildApi(config: BenchmarkConfig): OpenCodeApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(authInterceptor(config.username, config.password))
            .build()
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(config.baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenCodeApi::class.java)
    }

    private fun authInterceptor(username: String, password: String): Interceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .build(),
        )
    }

    private fun deterministicPayload(size: Int): ByteArray = ByteArray(size) { index -> ((index * 31) and 0xff).toByte() }

    private fun MessageWrapperDto.extractOutput(): String = buildString {
        parts.forEach { part ->
            part.text?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.output?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.raw?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.error?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        }
    }

    private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private data class BenchmarkConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val directory: String?,
) {
    companion object {
        fun fromEnvironment(): BenchmarkConfig = BenchmarkConfig(
            baseUrl = setting("OFISH_BENCH_BASE_URL") ?: "http://localhost:4096",
            username = setting("OFISH_BENCH_USERNAME") ?: "opencode",
            password = setting("OFISH_BENCH_PASSWORD") ?: "hunter2",
            directory = setting("OFISH_BENCH_DIRECTORY"),
        )

        private fun setting(name: String): String? = System.getProperty(name)
            ?: System.getenv(name)
    }
}

private data class BenchmarkResult(
    val bytes: Int,
    val elapsedMs: Long,
    val success: Boolean,
    val message: String,
) {
    val bytesPerSecond: Double = if (success && elapsedMs > 0) bytes * 1000.0 / elapsedMs else 0.0

    fun summary(): String = "${if (success) "SUCCESS" else "FAIL"} bytes=$bytes elapsedMs=$elapsedMs throughput=${"%.2f".format(bytesPerSecond / (1024 * 1024))}MiB/s message=${message.trim()}"
}

class ChunkSizeBenchmarkManualTest {
    @Ignore("Manual benchmark: run ChunkSizeBenchmark.main manually; normal tests must not contact opencode serve")
    @Test
    fun runManualBenchmark() = runBlocking {
        ChunkSizeBenchmark.run()
    }
}
