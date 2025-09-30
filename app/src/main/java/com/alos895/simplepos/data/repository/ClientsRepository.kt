package com.alos895.simplepos.data.repository

import com.alos895.simplepos.db.ClientDao
import com.alos895.simplepos.db.entity.ClientEntity
import com.alos895.simplepos.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClientsRepository(private val clientDao: ClientDao) {

    fun observeClients(): Flow<List<User>> = clientDao.observeClients().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun addUser(user: User) {
        clientDao.upsert(user.toEntity())
    }

    suspend fun removeUser(id: Long) {
        clientDao.deleteById(id)
    }

    private fun ClientEntity.toModel(): User = User(
        id = id,
        nombre = nombre,
        telefono = telefono
    )

    private fun User.toEntity(): ClientEntity = ClientEntity(
        id = if (id != 0L) id else 0,
        nombre = nombre,
        telefono = telefono
    )
}
