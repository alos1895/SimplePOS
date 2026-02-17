package com.alos895.simplepos.model

enum class PizzaBaseSize(val key: String, val displayName: String) {
    CHICA("CHICA", "Chica"),
    MEDIANA("MEDIANA", "Mediana"),
    GRANDE("GRANDE", "Grande");

    companion object {
        fun fromSizeLabel(label: String): PizzaBaseSize? {
            val normalized = label.trim().lowercase()
            return when {
                normalized.contains("chica") -> CHICA
                normalized.contains("mediana") -> MEDIANA
                normalized.contains("grande") -> GRANDE
                else -> null
            }
        }

        fun fromKey(key: String): PizzaBaseSize? = entries.firstOrNull { it.key == key }
    }
}
