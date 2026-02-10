package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alos895.simplepos.db.entity.ExtraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraDao {
    @Query("SELECT * FROM extras WHERE type = :type ORDER BY name ASC")
    fun getExtrasByType(type: String): Flow<List<ExtraEntity>>

    @Query("SELECT COUNT(*) FROM extras")
    suspend fun countExtras(): Long

    @Query("SELECT COUNT(*) FROM extras WHERE type = :type")
    suspend fun countExtrasByType(type: String): Long

    @Query("SELECT MAX(id) FROM extras WHERE type = :type")
    suspend fun maxExtraId(type: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtra(extra: ExtraEntity)

    @Update
    suspend fun updateExtra(extra: ExtraEntity)

    @Delete
    suspend fun deleteExtra(extra: ExtraEntity)
}
