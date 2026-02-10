package com.alos895.simplepos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.ExtraEntity
import com.alos895.simplepos.db.entity.IngredientEntity
import com.alos895.simplepos.db.entity.PizzaEntity
import com.alos895.simplepos.db.entity.PizzaSizeEntity

@Database(
    entities = [
        OrderEntity::class,
        TransactionEntity::class,
        IngredientEntity::class,
        PizzaEntity::class,
        PizzaSizeEntity::class,
        ExtraEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun cashTransactionDao(): CashTransactionDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun pizzaDao(): PizzaDao
    abstract fun extraDao(): ExtraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_pos_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
