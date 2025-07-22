package com.alos895.simplepos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.alos895.simplepos.model.OrderEntity

@Database(entities = [OrderEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
}

