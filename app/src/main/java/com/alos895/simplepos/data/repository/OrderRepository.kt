package com.alos895.simplepos.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.OrderItemEntity
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPostre
import com.alos895.simplepos.model.PaymentPart
import com.alos895.simplepos.model.sizeLabel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class OrderRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context.applicationContext)
    private val orderDao = db.orderDao()
    private val orderItemDao = db.orderItemDao()
    private val gson = Gson()

    suspend fun addOrder(order: OrderEntity): OrderEntity {
        return db.withTransaction {
            // 1. Calcular número del día
            val (startOfDay, endOfDay) = calculateDayBounds(order.timestamp)
            val nextNumber = (orderDao.getMaxDailyOrderNumberForRange(startOfDay, endOfDay) ?: 0) + 1
            val orderWithNumber = order.copy(dailyOrderNumber = nextNumber)
            
            // 2. Insertar la Orden principal
            val newId = orderDao.insertOrder(orderWithNumber)
            
            // 3. Normalizar: Convertir JSONs a filas de la tabla order_items
            val items = parseItems(newId, order.itemsJson, order.dessertsJson)
            orderItemDao.insertOrderItems(items)
            
            orderWithNumber.copy(id = newId)
        }
    }

    private fun parseItems(orderId: Long, itemsJson: String, dessertsJson: String): List<OrderItemEntity> {
        val result = mutableListOf<OrderItemEntity>()
        
        // Parsear Pizzas
        val pizzaType = object : TypeToken<List<CartItem>>() {}.type
        val pizzas: List<CartItem> = try { gson.fromJson(itemsJson, pizzaType) } catch (e: Exception) { emptyList() }
        
        pizzas.forEach { item ->
            result.add(OrderItemEntity(
                orderId = orderId,
                name = item.pizza?.nombre ?: if (item.isCombo) "Pizza Combinada" else "Pizza",
                type = if (item.isCombo) "COMBINADA" else "PIZZA",
                size = item.sizeLabel,
                quantity = item.cantidad,
                unitPrice = item.unitPrice ?: item.tamano?.precioBase ?: 0.0,
                subtotal = item.subtotal,
                flavor = item.pizza?.nombre,
                isCombined = item.isCombo
            ))
        }

        // Parsear Postres, Bebidas, etc.
        val dessertType = object : TypeToken<List<CartItemPostre>>() {}.type
        val desserts: List<CartItemPostre> = try { gson.fromJson(dessertsJson, dessertType) } catch (e: Exception) { emptyList() }
        
        desserts.forEach { item ->
            val type = when {
                item.postreOrExtra.esCombo -> "COMBO"
                item.postreOrExtra.esBebida -> "BEBIDA"
                item.postreOrExtra.esPostre -> "POSTRE"
                else -> "EXTRA"
            }
            result.add(OrderItemEntity(
                orderId = orderId,
                name = item.postreOrExtra.nombre,
                type = type,
                size = null,
                quantity = item.cantidad,
                unitPrice = item.postreOrExtra.precio,
                subtotal = item.subtotal,
                flavor = null,
                isCombined = false
            ))
        }
        
        return result
    }

    suspend fun getOrdersByDate(date: Long): List<OrderEntity> {
        return orderDao.getOrdersByDate(date)
    }

    suspend fun getOrdersByDateRange(start: Long, end: Long): List<OrderEntity> {
        return orderDao.getOrdersForDateRange(start, end)
    }

    suspend fun updatePaymentBreakdown(orderId: Long, paymentParts: List<PaymentPart>) {
        val paymentPartsJson = gson.toJson(paymentParts)
        orderDao.updatePaymentBreakdown(orderId, paymentPartsJson)
    }

    suspend fun clearPaymentBreakdown(orderId: Long) {
        orderDao.clearPaymentBreakdown(orderId)
    }

    suspend fun deleteOrderLogical(orderId: Long) {
        val (order, _) = getOrderById(orderId)
        if (order != null) {
            val updatedOrder = order.copy(isDeleted = true)
            updateOrder(updatedOrder)
        }
    }

    suspend fun getOrderById(orderId: Long): Pair<OrderEntity?, Boolean> {
        val order = orderDao.getOrderById(orderId)
        if (order == null) {
            return Pair(null, false)
        }
        return Pair(order, calculatePaymentStatus(order))
    }

    suspend fun updateOrder(order: OrderEntity) {
        db.withTransaction {
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
            // Nota: Aquí se podría re-sincronizar order_items si los items cambiaron
        }
    }

    fun calculatePaymentStatus(order: OrderEntity): Boolean {
        val type = object : TypeToken<List<PaymentPart>>() {}.type
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
