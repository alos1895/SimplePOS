package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alos895.simplepos.db.entity.BaseInventoryEntity
import com.alos895.simplepos.db.entity.BaseStockTotal
import com.alos895.simplepos.db.entity.DailyBaseInventorySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface BaseInventoryDao {
    @Insert
    suspend fun insertEntry(entry: BaseInventoryEntity)

    @Insert
    suspend fun insertEntries(entries: List<BaseInventoryEntity>)

    @Query(
        """
        SELECT sizeKey, COALESCE(SUM(quantity), 0) AS total
        FROM base_inventory
        WHERE dateKey = :dateKey
        GROUP BY sizeKey
        """
    )
    fun observeStockForDate(dateKey: String): Flow<List<BaseStockTotal>>

    @Query(
        """
        SELECT dateKey,
               COALESCE(SUM(CASE WHEN sizeKey = 'CHICA' THEN quantity ELSE 0 END), 0) AS chica,
               COALESCE(SUM(CASE WHEN sizeKey = 'MEDIANA' THEN quantity ELSE 0 END), 0) AS mediana,
               COALESCE(SUM(CASE WHEN sizeKey = 'GRANDE' THEN quantity ELSE 0 END), 0) AS grande
        FROM base_inventory
        GROUP BY dateKey
        ORDER BY dateKey DESC
        """
    )
    fun observeDailySummary(): Flow<List<DailyBaseInventorySummary>>

    @Query(
        """
        SELECT COALESCE(SUM(quantity), 0)
        FROM base_inventory
        WHERE dateKey = :dateKey AND sizeKey = :sizeKey
        """
    )
    suspend fun getAvailableForDateAndSize(dateKey: String, sizeKey: String): Int
}
