package com.alos895.simplepos.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alos895.simplepos.db.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun countIngredients(): Long

    @Query("SELECT MAX(id) FROM ingredients")
    suspend fun maxIngredientId(): Int?

    @Insert
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)
}
