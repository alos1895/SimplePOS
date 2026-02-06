package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val priceExtraSmall: Double,
    val priceExtraMedium: Double,
    val priceExtraLarge: Double
)
