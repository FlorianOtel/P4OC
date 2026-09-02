package dev.blazelight.p4oc.data.files.ofish

internal data class OfishCapabilities(
    val hasBase64: Boolean = false,
    val base64DecodeFlag: String? = null,
    val hashCommand: HashCommand? = null,
    val hasMv: Boolean = false,
    val hasMkdir: Boolean = false,
    val hasRm: Boolean = false,
    val hasAwk: Boolean = false,
    val hasMktemp: Boolean = false,
    val hasChmod: Boolean = false,
    val modeCommand: ModeCommand? = null,
) {
    val supportsMutation: Boolean
        get() = hasBase64 &&
            !base64DecodeFlag.isNullOrBlank() &&
            hashCommand != null &&
            hasMv &&
            hasMkdir &&
            hasRm &&
            hasAwk &&
            hasMktemp &&
            hasChmod &&
            modeCommand != null
}

internal enum class ModeCommand(val wireName: String) {
    STAT_GNU("stat -c %a"),
    STAT_BSD("stat -f %Lp"),
}

internal enum class HashCommand(val wireName: String) {
    SHA256SUM("sha256sum"),
    SHASUM_256("shasum -a 256"),
    MD5SUM("md5sum"),
}

internal sealed interface OfishProbeResult {
    data class Available(val capabilities: OfishCapabilities) : OfishProbeResult
    data class Missing(
        val missing: List<String>,
        val partial: OfishCapabilities?,
    ) : OfishProbeResult

    data class Failed(
        val message: String,
        val cause: Throwable? = null,
    ) : OfishProbeResult
}
