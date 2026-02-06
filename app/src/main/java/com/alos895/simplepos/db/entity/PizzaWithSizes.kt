package com.alos895.simplepos.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PizzaWithSizes(
    @Embedded val pizza: PizzaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "pizzaId"
    )
    val sizes: List<PizzaSizeEntity>
)
