package com.alos895.simplepos.data.repository

import com.alos895.simplepos.db.AppDatabase
import com.alos895.simplepos.db.entity.BaseInventoryEntity
import com.alos895.simplepos.model.PizzaBaseSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BaseInventoryStock(
    val chica: Int = 0,
    val mediana: Int = 0,
    val grande: Int = 0
) {
    fun availableFor(size: PizzaBaseSize): Int = when (size) {
        PizzaBaseSize.CHICA -> chica
        PizzaBaseSize.MEDIANA -> mediana
        PizzaBaseSize.GRANDE -> grande
    }

    fun hasStock(size: PizzaBaseSize, quantity: Int = 1): Boolean = availableFor(size) >= quantity
}

data class DailyBaseInventory(
    val dateKey: String,
    val chica: Int,
    val mediana: Int,
    val grande: Int
) {
    val displayDate: String
        get() = try {
            val parsed = DATE_KEY_FORMAT.parse(dateKey)
            if (parsed != null) DISPLAY_DATE_FORMAT.format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }

    companion object {
        private val DISPLAY_DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        private val DATE_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}

class BaseInventoryRepository(private val database: AppDatabase) {
    private val dao = database.baseInventoryDao()

    fun observeStockForDate(dateKey: String): Flow<BaseInventoryStock> =
        dao.observeStockForDate(dateKey).map { totals ->
            var chica = 0
            var mediana = 0
            var grande = 0
            totals.forEach { row ->
                when (PizzaBaseSize.fromKey(row.sizeKey)) {
                    PizzaBaseSize.CHICA -> chica = row.total
                    PizzaBaseSize.MEDIANA -> mediana = row.total
                    PizzaBaseSize.GRANDE -> grande = row.total
                    null -> Unit
                }
            }
            BaseInventoryStock(chica = chica, mediana = mediana, grande = grande)
        }

    fun observeDailySummary(): Flow<List<DailyBaseInventory>> =
        dao.observeDailySummary().map { rows ->
            rows.map { row ->
                DailyBaseInventory(
                    dateKey = row.dateKey,
                    chica = row.chica,
                    mediana = row.mediana,
                    grande = row.grande
                )
            }
        }

    suspend fun addBases(dateKey: String, chica: Int, mediana: Int, grande: Int) {
        val entries = buildList {
            if (chica > 0) {
                add(BaseInventoryEntity(dateKey = dateKey, sizeKey = PizzaBaseSize.CHICA.key, quantity = chica))
            }
            if (mediana > 0) {
                add(BaseInventoryEntity(dateKey = dateKey, sizeKey = PizzaBaseSize.MEDIANA.key, quantity = mediana))
            }
            if (grande > 0) {
                add(BaseInventoryEntity(dateKey = dateKey, sizeKey = PizzaBaseSize.GRANDE.key, quantity = grande))
            }
        }
        if (entries.isNotEmpty()) {
            dao.insertEntries(entries)
        }
    }

    suspend fun hasStock(dateKey: String, size: PizzaBaseSize, quantity: Int = 1): Boolean {
        val available = dao.getAvailableForDateAndSize(dateKey, size.key)
        return available >= quantity
    }

    companion object {
        fun toDateKey(timestamp: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return formatter.format(Date(timestamp))
        }
    }
}
