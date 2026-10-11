package dev.blazelight.p4oc.ui.theme

import kotlin.math.roundToInt

/**
 * App-wide interface scale, as a percentage of the device's display density.
 *
 * MainActivity applies it as a `densityDpi` configuration override, so every dp and sp
 * (Compose, dialogs, popups, sheets and embedded Android views) scales together.
 * Steps are 10%: each change recreates the activity, so steps must be visibly distinct.
 */
object InterfaceScale {
    const val DEFAULT_PERCENT = 100
    const val MIN_PERCENT = 80
    const val MAX_PERCENT = 150
    const val STEP_PERCENT = 10

    /** The percentage that leaves the device density unchanged. */
    private const val UNSCALED_PERCENT = 100f

    /** Missing, out-of-range or off-step stored values are treated as corrupt and fall back to 100%. */
    fun sanitize(percent: Int?): Int = percent
        ?.takeIf { it in MIN_PERCENT..MAX_PERCENT && (it - MIN_PERCENT) % STEP_PERCENT == 0 }
        ?: DEFAULT_PERCENT

    fun scaledDensityDpi(baseDensityDpi: Int, percent: Int): Int =
        (baseDensityDpi * percent / UNSCALED_PERCENT).roundToInt()
}
