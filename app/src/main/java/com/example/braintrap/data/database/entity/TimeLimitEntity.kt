package com.example.braintrap.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_limits")
data class TimeLimitEntity(
    @PrimaryKey
    val packageName: String,
    val dailyLimitMinutes: Int,
    val weekdayLimitMinutes: Int? = null,
    val weekendLimitMinutes: Int? = null,
    val isEnabled: Boolean = true
)

