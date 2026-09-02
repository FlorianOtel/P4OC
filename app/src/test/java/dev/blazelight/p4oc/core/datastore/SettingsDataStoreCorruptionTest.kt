package dev.blazelight.p4oc.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreCorruptionTest {
    @Test
    fun `corrupt settings are replaced with empty preferences`() = runTest {
        val replacement = settingsCorruptionHandler.handleCorruption(
            CorruptionException("corrupt settings"),
        )

        assertTrue(replacement.asMap().isEmpty())
    }
}
