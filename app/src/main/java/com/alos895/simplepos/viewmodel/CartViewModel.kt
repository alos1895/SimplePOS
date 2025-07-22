package com.alos895.simplepos.viewmodel

import androidx.lifecycle.ViewModel
import com.alos895.simplepos.data.PizzeriaData
import com.alos895.simplepos.model.CartItem
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.TamanoPizza
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    fun addToCart(pizza: Pizza, tamano: TamanoPizza) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.pizza.nombre == pizza.nombre && it.tamano.nombre == tamano.nombre }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(cantidad = item.cantidad + 1)
        } else {
            current.add(CartItem(pizza, tamano))
        }
        _cartItems.value = current
    }

    fun removeFromCart(pizza: Pizza, tamano: TamanoPizza) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.pizza.nombre == pizza.nombre && it.tamano.nombre == tamano.nombre }
        if (index >= 0) {
            val item = current[index]
            if (item.cantidad > 1) {
                current[index] = item.copy(cantidad = item.cantidad - 1)
            } else {
                current.removeAt(index)
            }
        }
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    val total: Double
        get() = _cartItems.value.sumOf { it.subtotal }

    fun buildTicket(): String {
        val cartItems = _cartItems.value
        if (cartItems.isEmpty()) {
            return "El carrito está vacío."
        }
        var result = 0.0
        val info = PizzeriaData.info
        val sb = StringBuilder()
        sb.appendLine(info.logoAscii)
        sb.appendLine(info.nombre)
        sb.appendLine(info.telefono)
        sb.appendLine(info.direccion)
        sb.appendLine("-------------------------------")
        cartItems.forEach { item ->
            sb.appendLine("${item.cantidad}x ${item.pizza.nombre} ${item.tamano.nombre}   $${"%.2f".format(item.subtotal)}")
            result += item.subtotal
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL: $${"%.2f".format(result)}")
        sb.appendLine("¡Gracias por su compra!")
        return sb.toString()
    }
}
