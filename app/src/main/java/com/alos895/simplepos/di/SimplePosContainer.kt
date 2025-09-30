package com.alos895.simplepos.di

import android.content.Context
import com.alos895.simplepos.data.repository.ClientsRepository
import com.alos895.simplepos.data.repository.MenuRepository
import com.alos895.simplepos.data.repository.OrderRepository
import com.alos895.simplepos.data.repository.TransactionsRepository
import com.alos895.simplepos.db.AppDatabase

interface SimplePosContainer {
    val menuRepository: MenuRepository
    val orderRepository: OrderRepository
    val transactionsRepository: TransactionsRepository
    val clientsRepository: ClientsRepository
}

class DefaultSimplePosContainer(context: Context) : SimplePosContainer {

    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    override val menuRepository: MenuRepository by lazy { MenuRepository() }

    override val orderRepository: OrderRepository by lazy {
        OrderRepository(database.orderDao())
    }

    override val transactionsRepository: TransactionsRepository by lazy {
        TransactionsRepository(database.cashTransactionDao())
    }

    override val clientsRepository: ClientsRepository by lazy {
        ClientsRepository(database.clientDao())
    }
}
