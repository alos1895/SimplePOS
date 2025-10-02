package com.alos895.simplepos.model

data class DeliveryService(
    val price: Int = 0,
    val description: String,
    val zona: String,
    val pickUp: Boolean = false,
    val type: DeliveryType
)

