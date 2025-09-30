package com.alos895.simplepos.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alos895.simplepos.ui.caja.CajaViewModel
import com.alos895.simplepos.ui.clients.UserViewModel
import com.alos895.simplepos.ui.menu.CartViewModel
import com.alos895.simplepos.ui.menu.MenuViewModel
import com.alos895.simplepos.ui.orders.OrderViewModel
import com.alos895.simplepos.ui.transaction.TransactionViewModel

class SimplePosViewModelFactory(
    private val container: SimplePosContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(MenuViewModel::class.java) -> {
                MenuViewModel(container.menuRepository)
            }

            modelClass.isAssignableFrom(CartViewModel::class.java) -> {
                CartViewModel(
                    menuRepository = container.menuRepository,
                    orderRepository = container.orderRepository
                )
            }

            modelClass.isAssignableFrom(OrderViewModel::class.java) -> {
                OrderViewModel(
                    orderRepository = container.orderRepository,
                    menuRepository = container.menuRepository
                )
            }

            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(container.transactionsRepository)
            }

            modelClass.isAssignableFrom(CajaViewModel::class.java) -> {
                CajaViewModel(
                    orderRepository = container.orderRepository,
                    transactionsRepository = container.transactionsRepository
                )
            }

            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(container.clientsRepository)
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
