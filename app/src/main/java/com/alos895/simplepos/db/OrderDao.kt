package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.DeliveryType

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE timestamp >= :day AND timestamp < :day + 86400000 AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getOrdersByDate(day: Long): List<OrderEntity>

    @Query("SELECT MAX(dailyOrderNumber) FROM orders WHERE timestamp >= :start AND timestamp < :end AND isDeleted = 0")
    suspend fun getMaxDailyOrderNumberForRange(start: Long, end: Long): Int?


    @Query("UPDATE orders SET itemsJson = :itemsJson, total = :total, timestamp = :timestamp, dailyOrderNumber = :dailyOrderNumber, userJson = :userJson, deliveryServicePrice = :deliveryServicePrice, isDeliveried = :isDeliveried, isWalkingDelivery = :isWalkingDelivery, dessertsJson = :dessertsJson, comentarios = :comentarios, deliveryAddress = :deliveryAddress, pizzaStatus = :pizzaStatus, paymentBreakdownJson= :paymentBreakdownJson, isDeleted = :isDeleted, deliveryType = :deliveryType, isTOTODO = :isTOTODO, precioTOTODO = :precioTOTODO, descuentoTOTODO = :descuentoTOTODO WHERE id = :id")
    suspend fun updateOrder(
        id: Long,
        itemsJson: String,
        total: Double,
        timestamp: Long,
        dailyOrderNumber: Int,
        userJson: String,
        deliveryServicePrice: Int,
        isDeliveried: Boolean,
        isWalkingDelivery: Boolean,
        dessertsJson: String,
        comentarios: String,
        deliveryAddress: String,
        pizzaStatus: String,
        isDeleted: Boolean,
        paymentBreakdownJson : String,
        deliveryType: DeliveryType,
        isTOTODO: Boolean,
        precioTOTODO: Double,
        descuentoTOTODO: Double
    )

    @Query("UPDATE orders SET paymentBreakdownJson = :paymentBreakdownJson WHERE id = :id")
    suspend fun updatePaymentBreakdown(
        id: Long,
        paymentBreakdownJson: String
    )

    @Query("UPDATE orders SET paymentBreakdownJson = '[]' WHERE id = :id")
    suspend fun clearPaymentBreakdown(
        id: Long
    )
}
