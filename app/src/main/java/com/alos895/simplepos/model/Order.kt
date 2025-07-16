package com.alos895.simplepos.model

data class Order(
    val items: List<CartItem>
) {
    val total: Double get() = items.sumOf { it.subtotal }
} 