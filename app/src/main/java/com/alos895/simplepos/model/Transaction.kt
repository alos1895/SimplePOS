package com.alos895.simplepos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

// TypeConverter for Date
class DateConverter {
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}

// TypeConverter for TransactionType
class TransactionTypeConverter {
    @TypeConverter
    fun toString(transactionType: TransactionType?): String? {
        return transactionType?.name
    }

    @TypeConverter
    fun toTransactionType(name: String?): TransactionType? {
        return name?.let { TransactionType.valueOf(it) }
    }
}

@Entity(tableName = "transactions") // Define table name
@TypeConverters(DateConverter::class, TransactionTypeConverter::class) // Register TypeConverters
data class Transaction(
    @PrimaryKey(autoGenerate = true) // Make id an auto-generated primary key
    val id: Long = 0, // Default value needed for autoGenerate
    val date: Date,
    val concept: String,
    val description: String,
    val total: Double,
    val type: TransactionType
)

enum class TransactionType {
    INCOME, // Ingreso
    EXPENSE // Gasto
}
