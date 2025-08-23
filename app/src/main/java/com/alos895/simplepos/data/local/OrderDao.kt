package com.alos895.simplepos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alos895.simplepos.model.OrderEntity

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("UPDATE orders SET itemsJson = :itemsJson, total = :total, timestamp = :timestamp, userJson = :userJson, deliveryServicePrice = :deliveryServicePrice, isDeliveried = :isDeliveried, dessertsJson = :dessertsJson, comentarios = :comentarios, deliveryAddress = :deliveryAddress, pizzaStatus = :pizzaStatus, paymentMethod = :paymentMethod, isDeleted = :isDeleted WHERE id = :id")
    suspend fun updateOrder(
        id: Long,
        itemsJson: String,
        total: Double,
        timestamp: Long,
        userJson: String,
        deliveryServicePrice: Int,
        isDeliveried: Boolean,
        dessertsJson: String,
        comentarios: String,
        deliveryAddress: String,
        pizzaStatus: String,
        paymentMethod: String,
        isDeleted: Boolean
    )
}
