package com.alos895.simplepos.data.local

import androidx.databinding.adapters.Converters
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alos895.simplepos.model.OrderEntity
import com.alos895.simplepos.model.Transaction

@Database(entities = [OrderEntity::class, Transaction::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao
}
