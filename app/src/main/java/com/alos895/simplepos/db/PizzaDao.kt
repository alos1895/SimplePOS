package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.alos895.simplepos.db.entity.PizzaEntity
import com.alos895.simplepos.db.entity.PizzaSizeEntity
import com.alos895.simplepos.db.entity.PizzaWithSizes
import kotlinx.coroutines.flow.Flow

@Dao
interface PizzaDao {
    @Transaction
    @Query("SELECT * FROM pizzas ORDER BY name ASC")
    fun getPizzasWithSizes(): Flow<List<PizzaWithSizes>>

    @Query("SELECT COUNT(*) FROM pizzas")
    suspend fun countPizzas(): Long

    @Insert
    suspend fun insertPizza(pizza: PizzaEntity): Long

    @Update
    suspend fun updatePizza(pizza: PizzaEntity)

    @Delete
    suspend fun deletePizza(pizza: PizzaEntity)

    @Insert
    suspend fun insertPizzaSizes(sizes: List<PizzaSizeEntity>)

    @Query("DELETE FROM pizza_sizes WHERE pizzaId = :pizzaId")
    suspend fun deleteSizesForPizza(pizzaId: Long)
}
