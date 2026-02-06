package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pizza_sizes",
    foreignKeys = [
        ForeignKey(
            entity = PizzaEntity::class,
            parentColumns = ["id"],
            childColumns = ["pizzaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pizzaId"])]
)
data class PizzaSizeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pizzaId: Long,
    val sizeName: String,
    val price: Double
)
