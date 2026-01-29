package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.PaymentPart
import com.google.gson.Gson
import java.util.Calendar

class OrderRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "simplepos.db"
    )
        .fallbackToDestructiveMigration(true)
        .build()

    private val orderDao = db.orderDao()

    suspend fun addOrder(order: OrderEntity): OrderEntity {
        return db.withTransaction {
            val (startOfDay, endOfDay) = calculateDayBounds(order.timestamp)
            val nextNumber = (orderDao.getMaxDailyOrderNumberForRange(startOfDay, endOfDay) ?: 0) + 1
            val orderWithNumber = order.copy(dailyOrderNumber = nextNumber)
            val newId = orderDao.insertOrder(orderWithNumber)
            orderWithNumber.copy(id = newId)
        }
    }

    suspend fun getOrdersByDate(date: Long): List<OrderEntity> {
        return orderDao.getOrdersByDate(date)
    }

    suspend fun updatePaymentBreakdown(orderId: Long, paymentParts: List<PaymentPart>) {
        val gson = Gson()
        val paymentPartsJson = gson.toJson(paymentParts)
        orderDao.updatePaymentBreakdown(orderId, paymentPartsJson)
    }

    suspend fun clearPaymentBreakdown(orderId: Long) {
        orderDao.clearPaymentBreakdown(orderId)
    }

    suspend fun deleteOrderLogical(orderId: Long) {
        val order = getOrderById(orderId).first
        if (order != null) {
            val updatedOrder = order.copy(isDeleted = true)
            updateOrder(updatedOrder)
        }
    }

    private suspend fun getOrderById(orderId: Long): Pair<OrderEntity?, Boolean> {
        val order = orderDao.getOrderById(orderId)
        if (order == null) {
            return Pair(null, false)
        }
        return Pair(order, calculatePaymentStatus(order))
    }

    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(
            id = order.id,
            itemsJson = order.itemsJson,
            total = order.total,
            timestamp = order.timestamp,
            dailyOrderNumber = order.dailyOrderNumber,
            userJson = order.userJson,
            deliveryServicePrice = order.deliveryServicePrice,
            isDeliveried = order.isDeliveried,
            isWalkingDelivery = order.isWalkingDelivery,
            dessertsJson = order.dessertsJson,
            comentarios = order.comentarios,
            deliveryAddress = order.deliveryAddress,
            pizzaStatus = order.pizzaStatus,
            isDeleted = order.isDeleted,
            paymentBreakdownJson = order.paymentBreakdownJson,
            deliveryType = order.deliveryType,
            isTOTODO = order.isTOTODO,
            precioTOTODO = order.precioTOTODO,
            descuentoTOTODO = order.descuentoTOTODO
        )
    }

    fun calculatePaymentStatus(order: OrderEntity): Boolean {
        val gson = Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<PaymentPart>>() {}.type

        val paymentParts: List<PaymentPart> = try {
            gson.fromJson(order.paymentBreakdownJson, type) ?: emptyList()
        } catch (e: Exception) {
            return false
        }

        val totalPaid = paymentParts.sumOf { it.amount }
        val epsilon = 0.001
        return totalPaid >= (order.total - epsilon)
    }

    private fun calculateDayBounds(timestamp: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + MILLIS_IN_DAY
        return startOfDay to endOfDay
    }

    private companion object {
        private const val MILLIS_IN_DAY = 86_400_000L
    }
}
