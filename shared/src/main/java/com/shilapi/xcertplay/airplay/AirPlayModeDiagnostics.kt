package com.shilapi.xcertplay.airplay

/** Only numeric/boolean resource ownership is retained; arbitrary phone payloads stay private. */
internal object AirPlayModeDiagnostics {
    fun summary(params: Map<String, Any?>): String {
        fun group(name: String, keys: List<String>): String =
            (params[name] as? List<*>)?.take(8)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                keys.mapNotNull { key ->
                    val value = map[key]
                    when (value) {
                        is Number -> "$key=${value.toInt()}"
                        is Boolean -> "$key=$value"
                        else -> null
                    }
                }.joinToString(",").takeIf { it.isNotEmpty() }
            }?.joinToString(";").orEmpty().ifEmpty { "unavailable" }
        return "AirPlay modes resources=[${group("resources", listOf("resourceID", "entity", "owner", "transferType"))}] " +
            "appStates=[${group("appStates", listOf("appStateID", "entity", "state", "speechMode"))}]"
    }
}
