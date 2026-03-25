package com.decathlon.smartnutristock.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converters for Room database.
 *
 * Converts between java.time.Instant and Long for storage in Room.
 * Room doesn't support java.time types natively, so we need converters.
 */
class InstantConverters {

    /**
     * Convert Instant to Long for database storage.
     *
     * @param instant The Instant to convert
     * @return Epoch milliseconds as Long
     */
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    /**
     * Convert Long from database to Instant.
     *
     * @param timestamp Epoch milliseconds
     * @return Instant object, or null if timestamp is null
     */
    @TypeConverter
    fun toInstant(timestamp: Long?): Instant? {
        return if (timestamp != null) {
            Instant.ofEpochMilli(timestamp)
        } else {
            null
        }
    }
}
