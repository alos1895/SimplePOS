package com.alos895.simplepos.db.entity

import androidx.room.Entity

@Entity(
    tableName = "extras",
    primaryKeys = ["id", "type"]
)
data class ExtraEntity(
    val id: Int,
    val name: String,
    val price: Double,
    val type: String
)
