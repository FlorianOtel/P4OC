package dev.blazelight.p4oc.core.security

import android.content.Context
import android.content.SharedPreferences
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class CredentialStoreRecoveryTest {
    private val context = mockk<Context>()

    @Test
    fun `first create failure deletes credential preferences and retries once`() {
        var creates = 0
        var deletes = 0

        CredentialStore(
            context = context,
            createPreferences = {
                creates += 1
                if (creates == 1) throw IOException("corrupt keyset")
                mockk<SharedPreferences>()
            },
            deletePreferences = {
                deletes += 1
            },
        )

        assertEquals(2, creates)
        assertEquals(1, deletes)
    }

    @Test
    fun `second create failure is surfaced without another reset`() {
        var creates = 0
        var deletes = 0
        val repeatedFailure = IOException("still corrupt")

        val thrown = assertThrows(IOException::class.java) {
            CredentialStore(
                context = context,
                createPreferences = {
                    creates += 1
                    if (creates == 1) throw IOException("corrupt keyset")
                    throw repeatedFailure
                },
                deletePreferences = {
                    deletes += 1
                },
            )
        }

        assertEquals(repeatedFailure, thrown)
        assertEquals(2, creates)
        assertEquals(1, deletes)
    }
}
