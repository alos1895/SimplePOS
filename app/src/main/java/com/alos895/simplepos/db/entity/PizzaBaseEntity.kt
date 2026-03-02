package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pizza_bases")
data class PizzaBaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val size: String,
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null
)
