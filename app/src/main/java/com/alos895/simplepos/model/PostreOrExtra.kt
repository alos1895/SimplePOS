package com.alos895.simplepos.model

data class PostreOrExtra(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val esPostre: Boolean = true,
    val esCombo: Boolean = false
)
