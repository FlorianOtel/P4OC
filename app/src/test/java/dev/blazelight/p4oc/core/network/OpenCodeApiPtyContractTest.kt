package dev.blazelight.p4oc.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

class OpenCodeApiPtyContractTest {
    @Test
    fun `PTY endpoints match upstream methods and paths`() {
        assertEndpoint("listPtySessions", GET::class.java, "pty")
        assertEndpoint("createPtySession", POST::class.java, "pty")
        assertEndpoint("getPtySession", GET::class.java, "pty/{id}")
        assertEndpoint("updatePtySession", PUT::class.java, "pty/{id}")
        assertEndpoint("deletePtySession", DELETE::class.java, "pty/{id}")
    }

    @Test
    fun `PTY endpoints declare explicit directory and workspace query scope`() {
        val methods = OpenCodeApi::class.java.declaredMethods.filter { "PtySession" in it.name }

        methods.forEach { method ->
            val parameterAnnotations = method.parameterAnnotations.flatten()
            assertEquals(
                listOf("directory", "workspace"),
                parameterAnnotations.filterIsInstance<Query>().map(Query::value),
            )
        }
    }

    @Test
    fun `PTY update keeps encoded path id separate from body and query scope`() {
        val method = OpenCodeApi::class.java.getDeclaredMethod(
            "updatePtySession",
            String::class.java,
            String::class.java,
            String::class.java,
            dev.blazelight.p4oc.data.remote.dto.UpdatePtyRequest::class.java,
            kotlin.coroutines.Continuation::class.java,
        )
        val annotations = method.parameterAnnotations.flatten()

        assertEquals(listOf("id"), annotations.filterIsInstance<Path>().map(Path::value))
        assertEquals(1, annotations.filterIsInstance<Body>().size)
    }

    private fun <T : Annotation> assertEndpoint(name: String, annotation: Class<T>, expectedPath: String) {
        val method = OpenCodeApi::class.java.declaredMethods.single { it.name == name }
        val endpoint = method.getAnnotation(annotation)
        assertNotNull(endpoint)
        val path = when (endpoint) {
            is GET -> endpoint.value
            is POST -> endpoint.value
            is PUT -> endpoint.value
            is DELETE -> endpoint.value
            else -> error("Unsupported endpoint annotation")
        }
        assertEquals(expectedPath, path)
    }
}
