package com.jacob77.bopsearch.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(value: QueueStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): QueueStatus = QueueStatus.valueOf(value)
}
