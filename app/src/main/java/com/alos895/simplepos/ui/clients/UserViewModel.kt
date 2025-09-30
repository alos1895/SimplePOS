package com.alos895.simplepos.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alos895.simplepos.data.repository.ClientsRepository
import com.alos895.simplepos.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class UserViewModel(
    private val clientsRepository: ClientsRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeClients()
    }

    private fun observeClients() {
        viewModelScope.launch {
            clientsRepository.observeClients()
                .onStart {
                    _isLoading.value = true
                    _error.value = null
                }
                .catch { throwable ->
                    _error.value = throwable.message ?: "Error al cargar clientes"
                    _isLoading.value = false
                }
                .collect { clients ->
                    _users.value = clients
                    _isLoading.value = false
                }
        }
    }

    fun addUser(user: User) {
        viewModelScope.launch {
            try {
                clientsRepository.addUser(user)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun removeUser(id: Long) {
        viewModelScope.launch {
            try {
                clientsRepository.removeUser(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
