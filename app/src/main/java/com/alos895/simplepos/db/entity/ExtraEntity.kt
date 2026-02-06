package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extras")
data class ExtraEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val price: Double,
    val type: String
)
