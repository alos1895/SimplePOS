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

@Database(
    entities = [
        OrderEntity::class,
        TransactionEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun cashTransactionDao(): CashTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val statements = listOf(
                    "ALTER TABLE orders ADD COLUMN isTOTODO INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE orders ADD COLUMN precioTOTODO REAL NOT NULL DEFAULT 0.0",
                    "ALTER TABLE orders ADD COLUMN descuentoTOTODO REAL NOT NULL DEFAULT 0.0",
                    "ALTER TABLE orders ADD COLUMN dailyOrderNumber INTEGER NOT NULL DEFAULT 0"
                )
                for (statement in statements) {
                    try {
                        database.execSQL(statement)
                    } catch (throwable: Throwable) {
                        if (throwable.message?.contains("duplicate column name", ignoreCase = true) != true) {
                            throw throwable
                        }
                    }
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN isWalkingDelivery INTEGER NOT NULL DEFAULT 0")
                } catch (throwable: Throwable) {
                    if (throwable.message?.contains("duplicate column name", ignoreCase = true) != true) {
                        throw throwable
                    }
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE orders ADD COLUMN deliveryType TEXT NOT NULL DEFAULT 'PASAN'")
                } catch (throwable: Throwable) {
                    if (throwable.message?.contains("duplicate column name", ignoreCase = true) != true) {
                        throw throwable
                    }
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_pos_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
