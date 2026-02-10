package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pizzas")
data class PizzaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredientIdsCsv: String,
    val isCombinable: Boolean
)
