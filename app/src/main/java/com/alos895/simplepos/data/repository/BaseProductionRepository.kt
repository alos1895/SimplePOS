package com.alos895.simplepos.data.repository

import android.content.Context
import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.BaseProductionTotals
import com.alos895.simplepos.db.entity.BaseProductionEntity

class BaseProductionRepository(context: Context) {
    private val baseProductionDao = AppDatabase.getDatabase(context).baseProductionDao()

    suspend fun insertBaseProduction(baseProduction: BaseProductionEntity) {
        baseProductionDao.insertBaseProduction(baseProduction)
    }

    suspend fun getAll(): List<BaseProductionEntity> {
        return baseProductionDao.getAll()
    }

    suspend fun getTotals(): BaseProductionTotals {
        return baseProductionDao.getTotals()
    }

    suspend fun deleteById(id: Long) {
        baseProductionDao.deleteById(id)
    }
}
