package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "base_production")
data class BaseProductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chicas: Int,
    val medianas: Int,
    val grandes: Int,
    val timestamp: Long
)
