package com.alos895.simplepos.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemsJson: String, // Serializa la lista de CartItem como JSON
    val total: Double,
    val timestamp: Long,
    val userJson: String
)
