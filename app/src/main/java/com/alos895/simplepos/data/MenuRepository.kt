package com.alos895.simplepos.data

import com.alos895.simplepos.model.Pizza

class MenuRepository {
    fun getPizzas(): List<Pizza> = MenuData.pizzas
} 