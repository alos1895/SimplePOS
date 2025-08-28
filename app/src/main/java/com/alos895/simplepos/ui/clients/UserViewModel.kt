package com.alos895.simplepos.ui.clients

import androidx.lifecycle.ViewModel
import com.alos895.simplepos.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    fun addUser(user: User) {
        _users.value = _users.value + user.copy(id = System.currentTimeMillis())
    }

    fun removeUser(id: Long) {
        _users.value = _users.value.filter { it.id != id }
    }
}