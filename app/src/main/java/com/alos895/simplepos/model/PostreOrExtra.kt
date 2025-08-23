package com.alos895.simplepos.model

data class PostreOrExtra(
    //TODO Add diferentiation between dessert and extra if needed
    val id: Int,
    val nombre: String,
    val precio: Double
)
