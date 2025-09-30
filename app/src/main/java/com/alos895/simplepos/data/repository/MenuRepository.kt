package com.alos895.simplepos.data.repository

import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.DeliveryService
import com.alos895.simplepos.model.Ingrediente
import com.alos895.simplepos.model.Pizza
import com.alos895.simplepos.model.PostreOrExtra

class MenuRepository {
    fun getPizzas(): List<Pizza> = MenuData.pizzas
    fun getDeliveryOptions(): List<DeliveryService> = MenuData.deliveryOptions
    fun getIngredientes(): List<Ingrediente> = MenuData.ingredientes
    fun getPostresOrExtras(): List<PostreOrExtra> = MenuData.postreOrExtras

    fun findIngredienteById(id: Int): Ingrediente? = MenuData.ingredientes.firstOrNull { it.id == id }

    fun getDefaultDelivery(): DeliveryService? = MenuData.deliveryOptions.firstOrNull()
}