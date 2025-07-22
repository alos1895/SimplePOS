package com.alos895.simplepos.viewmodel

import androidx.lifecycle.ViewModel
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    fun loadOrders() {
        _orders.value = repository.getOrders()
    }

    fun addOrder(order: Order) {
        repository.addOrder(order)
        loadOrders()
    }
}

