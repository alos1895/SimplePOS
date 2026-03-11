package com.alos895.simplepos.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                // Fallback para builds de desarrollo muy viejos (5, 6, etc.)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()

                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createOrderItemsTableIfMissing(db)
                ensureOrderColumns(db)
            }

            private fun createOrderItemsTableIfMissing(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `order_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `orderId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `size` TEXT,
                        `quantity` INTEGER NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `subtotal` REAL NOT NULL,
                        `flavor` TEXT,
                        `isCombined` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }

            private fun ensureOrderColumns(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "orders", "dailyOrderNumber", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "orders", "deliveryType", "TEXT NOT NULL DEFAULT 'PASAN'")
                addColumnIfMissing(db, "orders", "isDeleted", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "orders", "paymentBreakdownJson", "TEXT NOT NULL DEFAULT '[]'")
                addColumnIfMissing(db, "orders", "isTOTODO", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(db, "orders", "precioTOTODO", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(db, "orders", "descuentoTOTODO", "REAL NOT NULL DEFAULT 0.0")
            }

            private fun addColumnIfMissing(
                db: SupportSQLiteDatabase,
                tableName: String,
                columnName: String,
                columnDefinition: String
            ) {
                val columnExists = db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == columnName) {
                            return@use true
                        }
                    }
                    false
                }

                if (!columnExists) {
                    db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
                }
            }
        }
    }
}
