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
}
