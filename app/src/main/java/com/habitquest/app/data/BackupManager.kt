package com.habitquest.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    suspend fun exportToJson(context: Context): String {
        val db = AppDatabase.get(context)
        val habits = db.habitDao().getAll()
        val completions = db.completionDao().getAll()

        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val habitsArray = JSONArray()
        for (h in habits) {
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("title", h.title)
            obj.put("emoji", h.emoji)
            obj.put("color", h.color)
            obj.put("category", h.category)
            obj.put("targetDaysMask", h.targetDaysMask)
            obj.put("xpReward", h.xpReward)
            obj.put("reminderEnabled", h.reminderEnabled)
            obj.put("reminderHour", h.reminderHour)
            obj.put("reminderMinute", h.reminderMinute)
            obj.put("createdDay", h.createdDay)
            obj.put("isArchived", h.isArchived)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val completionsArray = JSONArray()
        for (c in completions) {
            val obj = JSONObject()
            obj.put("habitId", c.habitId)
            obj.put("day", c.day)
            completionsArray.put(obj)
        }
        root.put("completions", completionsArray)

        return root.toString(2)
    }

    suspend fun importFromJson(context: Context, jsonStr: String): Result<Int> {
        return try {
            val root = JSONObject(jsonStr)
            val db = AppDatabase.get(context)

            val habitsArray = root.optJSONArray("habits") ?: return Result.failure(IllegalArgumentException("No habits found in backup"))
            val completionsArray = root.optJSONArray("completions") ?: JSONArray()

            var importedHabits = 0
            val oldToNewIdMap = mutableMapOf<Long, Long>()

            for (i in 0 until habitsArray.length()) {
                val obj = habitsArray.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val habit = HabitEntity(
                    id = 0, // Fresh ID to prevent collision
                    title = obj.getString("title"),
                    emoji = obj.optString("emoji", "🌱"),
                    color = obj.optInt("color", 0xFFE07A5F.toInt()),
                    category = obj.optString("category", "General"),
                    targetDaysMask = obj.optInt("targetDaysMask", 127),
                    xpReward = obj.optInt("xpReward", 25),
                    reminderEnabled = obj.optBoolean("reminderEnabled", false),
                    reminderHour = obj.optInt("reminderHour", 8),
                    reminderMinute = obj.optInt("reminderMinute", 0),
                    createdDay = obj.optLong("createdDay", 0L),
                    isArchived = obj.optBoolean("isArchived", false)
                )
                val newId = db.habitDao().insert(habit)
                if (oldId > 0) {
                    oldToNewIdMap[oldId] = newId
                }
                importedHabits++
            }

            for (i in 0 until completionsArray.length()) {
                val obj = completionsArray.getJSONObject(i)
                val oldHabitId = obj.getLong("habitId")
                val day = obj.getLong("day")
                val mappedId = oldToNewIdMap[oldHabitId]
                if (mappedId != null) {
                    db.completionDao().insert(CompletionEntity(habitId = mappedId, day = day))
                }
            }

            Result.success(importedHabits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
