package com.alos895.simplepos.db

import androidx.room.TypeConverter
import com.alos895.simplepos.model.DeliveryType
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDeliveryType(value: DeliveryType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDeliveryType(value: String?): DeliveryType {
        return value?.let {
            runCatching { DeliveryType.valueOf(it) }.getOrDefault(DeliveryType.PASAN)
        } ?: DeliveryType.PASAN
    }
}