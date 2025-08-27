package com.alos895.simplepos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INGRESO,
    GASTO
}

@Entity(tableName = "cash_transactions")
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val concept: String,
    val amount: Double,
    val type: TransactionType,
    val date: Long // Almacenaremos la fecha como un timestamp (milisegundos)
)