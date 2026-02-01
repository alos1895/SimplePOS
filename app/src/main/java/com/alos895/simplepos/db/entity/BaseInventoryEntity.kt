package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "base_inventory")
data class BaseInventoryEntity(
    @PrimaryKey val dateKey: String,
    val baseGrandes: Int,
    val baseMedianas: Int,
    val baseChicas: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
