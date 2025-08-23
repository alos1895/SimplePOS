package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.Room
import com.alos895.simplepos.data.local.AppDatabase
import com.alos895.simplepos.model.OrderEntity

class OrderRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "simplepos.db"
    ).build()

    private val orderDao = db.orderDao()

    suspend fun addOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
    }

    suspend fun getOrders(): List<OrderEntity> {
        return orderDao.getAllOrders()
    }

    suspend fun deleteOrderLogical(orderId: Long) {
        val order = getOrderById(orderId)
        if (order != null) {
            val updatedOrder = order.copy(isDeleted = true)
            updateOrder(updatedOrder)
        }
    }

    private suspend fun getOrderById(orderId: Long): OrderEntity? {
        return orderDao.getOrderById(orderId)
    }

    private suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(
            id = order.id,
            itemsJson = order.itemsJson,
            total = order.total,
            timestamp = order.timestamp,
            userJson = order.userJson,
            deliveryServicePrice = order.deliveryServicePrice,
            isDeliveried = order.isDeliveried,
            dessertsJson = order.dessertsJson,
            comentarios = order.comentarios,
            deliveryAddress = order.deliveryAddress,
            pizzaStatus = order.pizzaStatus,
            paymentMethod = order.paymentMethod,
            isDeleted = order.isDeleted
        )
    }
}
