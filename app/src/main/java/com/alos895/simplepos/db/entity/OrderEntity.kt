package com.alos895.simplepos.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alos895.simplepos.model.DeliveryType

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemsJson: String,
    val total: Double,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0")
    val dailyOrderNumber: Int = 0,
    val userJson: String,
    val deliveryServicePrice: Int = 0,
    val isDeliveried: Boolean = false,
    val isWalkingDelivery: Boolean = false,
    val dessertsJson: String = "[]",
    val comentarios: String = "",
    val deliveryAddress: String = "",
    @ColumnInfo(defaultValue = "'PASAN'")
    val deliveryType: DeliveryType = DeliveryType.PASAN,
    val pizzaStatus: String = "",
    val isDeleted: Boolean = false,
    var paymentBreakdownJson: String = "[]",
    var isTOTODO: Boolean = false,
    var precioTOTODO: Double = 0.0,
    var descuentoTOTODO: Double = 0.0,
)
