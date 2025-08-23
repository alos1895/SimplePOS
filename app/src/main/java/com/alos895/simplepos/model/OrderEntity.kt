package com.alos895.simplepos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemsJson: String,
    val total: Double,
    val timestamp: Long,
    val userJson: String,
    val deliveryServicePrice: Int = 0,
    val isDeliveried: Boolean = false,
    val dessertsJson: String = "[]",
    val comentarios: String = "",
    val deliveryAddress: String = "",
    val pizzaStatus: String = "",
    val paymentMethod: String = "",
    val isDeleted: Boolean = false
)
