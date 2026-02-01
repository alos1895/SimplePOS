package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alos895.simplepos.db.entity.BaseInventoryEntity
import com.alos895.simplepos.db.entity.BaseInventoryTotals

@Dao
interface BaseInventoryDao {
    @Query("SELECT * FROM base_inventory WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDateKey(dateKey: String): BaseInventoryEntity?

    @Query(
        """
        SELECT
            COALESCE(SUM(baseGrandes), 0) AS totalGrandes,
            COALESCE(SUM(baseMedianas), 0) AS totalMedianas,
            COALESCE(SUM(baseChicas), 0) AS totalChicas
        FROM base_inventory
        """
    )
    suspend fun getTotals(): BaseInventoryTotals

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BaseInventoryEntity)
}
