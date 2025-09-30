package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY nombre ASC")
    fun observeClients(): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteById(id: Long)
}
