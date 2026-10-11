package dev.blazelight.p4oc.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceScaleTest {
    @Test
    fun `stored steps inside the range are kept`() {
        assertEquals(80, InterfaceScale.sanitize(80))
        assertEquals(130, InterfaceScale.sanitize(130))
        assertEquals(150, InterfaceScale.sanitize(150))
    }

    @Test
    fun `missing, out of range and off-step stored values fall back to 100 percent`() {
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(null))
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(70))
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(160))
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(0))
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(-120))
        assertEquals(InterfaceScale.DEFAULT_PERCENT, InterfaceScale.sanitize(115))
    }

    @Test
    fun `scaled density rounds to the nearest dpi`() {
        assertEquals(420, InterfaceScale.scaledDensityDpi(420, 100))
        assertEquals(546, InterfaceScale.scaledDensityDpi(420, 130))
        assertEquals(353, InterfaceScale.scaledDensityDpi(441, 80))
    }
}
