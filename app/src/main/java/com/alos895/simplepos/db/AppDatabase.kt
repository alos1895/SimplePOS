package com.alos895.simplepos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alos895.simplepos.db.entity.TransactionEntity
import com.alos895.simplepos.db.entity.OrderEntity
import com.alos895.simplepos.db.entity.ExtraEntity
import com.alos895.simplepos.db.entity.IngredientEntity
import com.alos895.simplepos.db.entity.PizzaEntity
import com.alos895.simplepos.db.entity.PizzaSizeEntity
import com.alos895.simplepos.db.entity.PizzaBaseEntity

@Database(
    entities = [
        OrderEntity::class,
        TransactionEntity::class,
        IngredientEntity::class,
        PizzaEntity::class,
        PizzaSizeEntity::class,
        ExtraEntity::class,
        PizzaBaseEntity::class
    ],
    version = 5,
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

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pizza_bases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `size` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `usedAt` INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change: keeps user data and aligns Room metadata/version.
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_pos_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
