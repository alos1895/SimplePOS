package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alos895.simplepos.db.entity.PizzaBaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PizzaBaseDao {
    @Query("SELECT * FROM pizza_bases ORDER BY createdAt DESC")
    fun getPizzaBases(): Flow<List<PizzaBaseEntity>>

    @Insert
    suspend fun insertPizzaBase(base: PizzaBaseEntity): Long

    @Update
    suspend fun updatePizzaBase(base: PizzaBaseEntity)

    @Query("UPDATE pizza_bases SET usedAt = :usedAt WHERE id = :baseId")
    suspend fun markAsUsed(baseId: Long, usedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM pizza_bases WHERE createdAt BETWEEN :startOfDayMillis AND :endOfDayMillis")
    suspend fun deleteByCreatedAtRange(startOfDayMillis: Long, endOfDayMillis: Long)

    @Query("SELECT COUNT(*) FROM pizza_bases WHERE size = :size AND usedAt IS NULL")
    suspend fun countAvailableBasesBySize(size: String): Int

    @Query(
        """
        UPDATE pizza_bases
        SET usedAt = :usedAt
        WHERE id IN (
            SELECT id FROM pizza_bases
            WHERE size = :size AND usedAt IS NULL
            ORDER BY createdAt ASC, id ASC
            LIMIT :count
        )
        """
    )
    suspend fun markOldestAvailableAsUsed(size: String, count: Int, usedAt: Long): Int
}
