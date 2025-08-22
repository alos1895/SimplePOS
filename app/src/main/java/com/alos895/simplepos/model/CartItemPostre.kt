package com.alos895.simplepos.model

data class CartItemPostre(
    val postre: Postre,
    val cantidad: Int = 1
) {
    val subtotal: Double
        get() = postre.precio * cantidad
}
