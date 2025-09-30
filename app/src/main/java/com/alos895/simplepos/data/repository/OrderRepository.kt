package com.alos895.simplepos.data.repository

import com.alos895.simplepos.db.OrderDao
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.model.PaymentPart
import com.google.gson.Gson

class OrderRepository(private val orderDao: OrderDao) {

    suspend fun addOrder(order: OrderEntity) {
        orderDao.insertOrder(order)
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
            userJson = order.userJson,
            deliveryServicePrice = order.deliveryServicePrice,
            isDeliveried = order.isDeliveried,
            dessertsJson = order.dessertsJson,
            comentarios = order.comentarios,
            deliveryAddress = order.deliveryAddress,
            pizzaStatus = order.pizzaStatus,
            isDeleted = order.isDeleted,
            paymentBreakdownJson = order.paymentBreakdownJson
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
}
