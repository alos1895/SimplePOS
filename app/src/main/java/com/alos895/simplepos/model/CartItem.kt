package com.alos895.simplepos.model

import java.util.UUID

enum class PizzaFractionType(val numerator: Int, val denominator: Int, val label: String) {
    WHOLE(1, 1, "1/1"),
    HALF(1, 2, "1/2"),
    THIRD(1, 3, "1/3"),
    QUARTER(1, 4, "1/4");

    val ratio: Double get() = numerator.toDouble() / denominator
}

data class CartItemPortion(
    val pizzaName: String,
    val fraction: PizzaFractionType
)

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val pizza: Pizza? = null,
    val tamano: TamanoPizza? = null,
    val sizeName: String? = null,
    val unitPrice: Double? = null,
    val portions: List<CartItemPortion> = emptyList(),
    val cantidad: Int = 1
) {
    val isCombo: Boolean get() = portions.isNotEmpty()

    val subtotal: Double
        get() = (unitPrice ?: tamano?.precioBase ?: 0.0) * cantidad
}

val CartItem.sizeLabel: String
    get() = sizeName ?: tamano?.nombre ?: ""

val CartItem.unitPriceSingle: Double
    get() = unitPrice ?: tamano?.precioBase ?: 0.0

val CartItem.displayName: String
    get() = pizza?.nombre ?: if (isCombo) "Pizza combinada" else "Pizza"
