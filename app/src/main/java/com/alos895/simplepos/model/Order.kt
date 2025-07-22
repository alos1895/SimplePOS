package com.alos895.simplepos.model

data class Order(
    val id: Long,
    val items: List<CartItem>,
    val total: Double,
    val timestamp: Long
)
