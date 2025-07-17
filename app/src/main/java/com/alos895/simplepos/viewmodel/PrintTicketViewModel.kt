package com.alos895.simplepos.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PrintTicketViewModel : ViewModel() {
    private val _ticket = MutableStateFlow("")
    val ticket: StateFlow<String> = _ticket

    fun setTicket(text: String) {
        _ticket.value = text
    }
}
