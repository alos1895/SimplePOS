package com.alos895.simplepos.model

enum class DeliveryType(
    val dbValue: String,
    val displayName: String
) {
    PASAN("PASAN", "Pasan"),
    CAMINANDO("CAMINANDO", "Caminando"),
    TOTODO("TOTODO", "TOTODO"),
    A_DOMICILIO("A_DOMICILIO", "A domicilio");

    companion object {
        fun fromDb(value: String): DeliveryType {
            return entries.firstOrNull { it.dbValue.equals(value.trim(), ignoreCase = true) }
                ?: PASAN
        }
    }
}
