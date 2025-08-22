package com.alos895.simplepos.model

data class CartItemPostre(
    val postreOrExtra: PostreOrExtra,
    val cantidad: Int = 1
) {
    val subtotal: Double
        get() = postreOrExtra.precio * cantidad
}
