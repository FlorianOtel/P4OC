package dev.blazelight.p4oc.data.remote.dto

import kotlinx.serialization.json.jsonObject

fun ModelDto.reasoningEfforts(): List<String> {
    val variants = variants ?: variant ?: options?.get("variants")?.jsonObject ?: options?.get("variant")?.jsonObject
    return variants?.keys.orEmpty().toList()
}
