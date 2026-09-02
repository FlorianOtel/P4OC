package dev.blazelight.p4oc.core.security

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CredentialBackupRulesTest {
    @Test
    fun `encrypted credential preferences are excluded from backup and transfer`() {
        val backupRules = resourceFile("backup_rules.xml").readText()
        val extractionRules = resourceFile("data_extraction_rules.xml").readText()
        val exclusion = "<exclude domain=\"sharedpref\" path=\"p4oc_credentials.xml\" />"

        assertTrue("Legacy backup rules must exclude credentials", exclusion in backupRules)
        assertTrue(
            "Cloud and device-transfer rules must both exclude credentials",
            extractionRules.windowed(exclusion.length).count { it == exclusion } == 2,
        )
    }

    private fun resourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/res/xml/$name"),
            File("app/src/main/res/xml/$name"),
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Could not locate $name from ${File(".").absolutePath}")
    }
}
