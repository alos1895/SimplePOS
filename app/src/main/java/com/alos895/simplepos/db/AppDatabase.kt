package com.alos895.simplepos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alos895.simplepos.db.entity.*

@Database(
    entities = [
        OrderEntity::class,
        TransactionEntity::class,
        IngredientEntity::class,
        PizzaEntity::class,
        PizzaSizeEntity::class,
        ExtraEntity::class,
        PizzaBaseEntity::class,
        OrderItemEntity::class
    ],
    version = 1, // Reset a Versión 1 para lanzamiento
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun cashTransactionDao(): CashTransactionDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun pizzaDao(): PizzaDao
    abstract fun extraDao(): ExtraDao
    abstract fun pizzaBaseDao(): PizzaBaseDao
    abstract fun orderItemDao(): OrderItemDao

    companion object {
        private const val DB_NAME = "simplepos.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                // Esto borrará versiones viejas de desarrollo (5, 6, etc.) y empezará de cero en la 1
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
