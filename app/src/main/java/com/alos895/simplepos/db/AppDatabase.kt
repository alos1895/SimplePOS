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
        OrderItemEntity::class // Nueva tabla
    ],
    version = 6, // Incrementado de 5 a 6
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
    abstract fun orderItemDao(): OrderItemDao // Nuevo DAO

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
                // Se eliminó fallbackToDestructiveMigration para evitar pérdida de datos
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
