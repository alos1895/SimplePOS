package com.alos895.simplepos.model // O donde tengas tus modelos

data class PaymentPart(
    val method: PaymentMethod,
    val amount: Double,
    val reference: String? = null
)
enum class PaymentMethod {
    EFECTIVO,
    TRANSFERENCIA,
}