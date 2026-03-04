package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.OrderItemEntity

@Dao
interface OrderItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT type, name, SUM(quantity) as quantity, SUM(subtotal) as sales FROM order_items GROUP BY type, name ORDER BY quantity DESC")
    suspend fun getGlobalProductRanking(): List<ProductStats>

    @Query("SELECT type, name, SUM(quantity) as quantity, SUM(subtotal) as sales FROM order_items INNER JOIN orders ON order_items.orderId = orders.id WHERE orders.timestamp >= :start AND orders.timestamp < :end AND orders.isDeleted = 0 GROUP BY type, name ORDER BY quantity DESC")
    suspend fun getProductRankingByDateRange(start: Long, end: Long): List<ProductStats>
}

data class ProductStats(
    val type: String,
    val name: String,
    val quantity: Int,
    val sales: Double
)
