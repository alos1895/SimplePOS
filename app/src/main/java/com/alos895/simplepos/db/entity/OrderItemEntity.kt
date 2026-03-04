package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val name: String,
    val type: String, // PIZZA, POSTRE, BEBIDA, COMBO, EXTRA
    val size: String?,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val flavor: String?, // Para pizzas
    val isCombined: Boolean = false
)
