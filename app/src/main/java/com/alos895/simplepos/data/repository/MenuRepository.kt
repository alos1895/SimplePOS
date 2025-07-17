package com.alos895.simplepos.data.repository

import com.alos895.simplepos.data.datasource.MenuData
import com.alos895.simplepos.model.Pizza

class MenuRepository {
    fun getPizzas(): List<Pizza> = MenuData.pizzas
} 