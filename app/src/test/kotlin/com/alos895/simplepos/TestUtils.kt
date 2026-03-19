package com.alos895.simplepos

import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.CartItemPortion
import com.alos895.simplepos.model.PaymentMethod
import com.alos895.simplepos.model.PaymentPart
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PizzaFractionType
import com.alos895.simplepos.model.TamanoPizza
import com.alos895.simplepos.model.User

/** Utilidades sencillas para crear modelos de prueba consistentes. */
object TestUtils {
    fun createUser(nombre: String = "Cliente Test", telefono: String = "555-1234"): User =
        User(id = 0, nombre = nombre, telefono = telefono)

    fun createPizza(
        nombre: String = "Pizza Test",
        tamanos: List<TamanoPizza> = listOf(TamanoPizza("Familiar", 120.0)),
        ingredientes: List<Int> = listOf(1, 2, 3)
    ): Pizza = Pizza(nombre = nombre, ingredientesBaseIds = ingredientes, tamanos = tamanos)

    fun createPortion(
        pizzaName: String = "Pizza Test",
        fraction: PizzaFractionType = PizzaFractionType.HALF
    ): CartItemPortion = CartItemPortion(pizzaName = pizzaName, fraction = fraction)

    fun createCartItem(
        pizza: Pizza = createPizza(),
        sizeName: String = "Familiar",
        unitPrice: Double = 120.0,
        quantity: Int = 1,
        isGolden: Boolean = false,
        portions: List<CartItemPortion> = emptyList()
    ): CartItem = CartItem(
        pizza = if (portions.isEmpty()) pizza else null,
        tamano = if (portions.isEmpty()) pizza.tamanos.first() else null,
        sizeName = if (portions.isEmpty()) null else sizeName,
        unitPrice = if (portions.isEmpty()) null else unitPrice,
        cantidad = quantity,
        isGolden = isGolden,
        portions = portions
    )

    fun createPaymentPart(method: PaymentMethod = PaymentMethod.EFECTIVO, amount: Double = 100.0): PaymentPart =
        PaymentPart(method = method, amount = amount)
}
