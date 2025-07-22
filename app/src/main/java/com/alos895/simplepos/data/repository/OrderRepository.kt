package com.alos895.simplepos.data.repository

import com.alos895.simplepos.model.Order

class OrderRepository {
    private val orders = mutableListOf<Order>()

    fun getOrders(): List<Order> = orders.toList()

    fun addOrder(order: Order) {
        orders.add(order)
    }
}

