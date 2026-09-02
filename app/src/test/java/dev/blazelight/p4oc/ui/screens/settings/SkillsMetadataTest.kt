package dev.blazelight.p4oc.ui.screens.settings

import dev.blazelight.p4oc.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsMetadataTest {

    @Test
    fun mcpStatusDescriptionRes_mapsKnownStatusesToStringResources() {
        val expectedResourcesByStatus = mapOf(
            MCP_STATUS_CONNECTED to R.string.skills_status_connected,
            "disabled" to R.string.skills_status_disabled,
            "failed" to R.string.skills_status_failed,
            "needs_auth" to R.string.skills_status_needs_auth,
            "needs_client_registration" to R.string.skills_status_needs_client_registration,
        )

        expectedResourcesByStatus.forEach { (status, expectedResource) ->
            assertEquals(expectedResource, mcpStatusDescriptionRes(status))
        }
    }

    @Test
    fun mcpStatusDescriptionRes_mapsUnknownStatusToUnknownStringResource() {
        assertEquals(R.string.skills_status_unknown, mcpStatusDescriptionRes("unexpected_status"))
    }

    @Test
    fun skillInfo_isEnabledOnlyForConnectedStatus() {
        assertTrue(skillInfo(status = MCP_STATUS_CONNECTED).isEnabled)
        assertFalse(skillInfo(status = "disabled").isEnabled)
    }

    @Test
    fun skillsErrorKind_exposesExpectedKindsForUiMapping() {
        assertEquals(setOf(SkillsErrorKind.NotConnected, SkillsErrorKind.ApiError), SkillsErrorKind.entries.toSet())
    }

    private fun skillInfo(status: String) = SkillInfo(
        name = "filesystem",
        status = status,
        source = "project",
    )
}
