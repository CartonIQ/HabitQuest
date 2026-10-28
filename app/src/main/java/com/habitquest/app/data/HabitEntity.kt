package com.habitquest.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String,
    val color: Int,
    val category: String = "General",
    val targetDaysMask: Int = 127,
    val xpReward: Int = 25,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val createdDay: Long,
    val isArchived: Boolean = false
)
