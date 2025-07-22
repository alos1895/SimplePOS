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
}

