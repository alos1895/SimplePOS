package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "base_inventory")
data class BaseInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val sizeKey: String,
    val quantity: Int,
    val createdAt: Long = System.currentTimeMillis()
)

data class BaseStockTotal(
    val sizeKey: String,
    val total: Int
)

data class DailyBaseInventorySummary(
    val dateKey: String,
    val chica: Int,
    val mediana: Int,
    val grande: Int
)
