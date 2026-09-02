package dev.blazelight.p4oc.ui.screens.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class ConnectFormValidationTest {
    @Test
    fun `a complete form has no error`() {
        val state = ServerUiState(remoteUrl = "http://box.local:4096", username = "opencode", password = "hunter2")

        assertNull(state.connectFormError())
    }

    @Test
    fun `the url is reported before the credentials`() {
        val state = ServerUiState(remoteUrl = "  ", username = "", password = "")

        assertEquals("Enter a server URL", state.connectFormError())
    }

    @Test
    fun `passwordless connections are accepted and password requires a username`() {
        val base = ServerUiState(remoteUrl = "http://box.local:4096")

        assertNull(base.connectFormError())
        assertNull(base.copy(username = "", password = "").connectFormError())
        assertEquals("Enter a username (usually opencode)", base.copy(username = "", password = "p").connectFormError())
        assertNull(base.copy(username = "opencode", password = "").connectFormError())
    }

    @Test
    fun `username defaults to opencode`() {
        assertEquals("opencode", ServerUiState().username)
    }

    @Test
    fun `tls options stay hidden until an attempt fails on trust`() {
        assertFalse(ServerUiState().showTlsOptions)
    }

    @Test
    fun `certificate failures are recognised through the cause chain`() {
        val wrapped = IOException("probe failed", SSLHandshakeException("cert path"))

        assertTrue(wrapped.isTlsTrustFailure())
        assertTrue(SSLPeerUnverifiedException("no peer").isTlsTrustFailure())
        assertTrue(CertificateException("bad cert").isTlsTrustFailure())
        assertTrue(CertPathValidatorException("untrusted anchor").isTlsTrustFailure())
    }

    @Test
    fun `ordinary connection failures are not treated as trust failures`() {
        assertFalse(SocketTimeoutException("timeout").isTlsTrustFailure())
        assertFalse(IOException("connection refused").isTlsTrustFailure())
    }

    @Test
    fun `a self-referencing cause chain terminates`() {
        val outer = IOException("outer")
        val inner = IOException("inner", outer)
        outer.initCause(inner)

        assertFalse(outer.isTlsTrustFailure())
    }
}
