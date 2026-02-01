package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.BaseInventoryEntity

@Dao
interface BaseInventoryDao {
    @Query("SELECT * FROM base_inventory WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDateKey(dateKey: String): BaseInventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BaseInventoryEntity)
}
