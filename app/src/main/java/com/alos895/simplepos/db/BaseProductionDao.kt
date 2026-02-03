package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alos895.simplepos.db.entity.BaseProductionEntity

data class BaseProductionTotals(
    val totalChicas: Long = 0,
    val totalMedianas: Long = 0,
    val totalGrandes: Long = 0
)

@Dao
interface BaseProductionDao {
    @Insert
    suspend fun insertBaseProduction(baseProduction: BaseProductionEntity)

    @Query(
        "SELECT " +
            "COALESCE(SUM(chicas), 0) as totalChicas, " +
            "COALESCE(SUM(medianas), 0) as totalMedianas, " +
            "COALESCE(SUM(grandes), 0) as totalGrandes " +
            "FROM base_production"
    )
    suspend fun getTotals(): BaseProductionTotals
}
