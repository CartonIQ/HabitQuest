package com.habitquest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.habitquest.app.data.AppDatabase
import com.habitquest.app.data.CompletionEntity
import com.habitquest.app.data.HabitEntity
import com.habitquest.app.rpg.LevelManager
import com.habitquest.app.rpg.LevelProgress
import com.habitquest.app.util.DateUtil
import com.habitquest.app.util.Prefs
import com.habitquest.app.util.ReminderScheduler
import com.habitquest.app.util.Streaks
import kotlinx.coroutines.launch

data class HabitUi(
    val habit: HabitEntity,
    val completedToday: Boolean,
    val streak: Int,
    val atRisk: Boolean,
    val isDueToday: Boolean
)

data class DeletedHabit(val habit: HabitEntity, val days: List<Long>)

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val habitsLd = db.habitDao().observeAll()
    private val completionsLd = db.completionDao().observeAll()

    private var habits: List<HabitEntity>? = null
    private var completions: List<CompletionEntity>? = null

    val items = MediatorLiveData<List<HabitUi>>()
    val milestone = MutableLiveData<Int?>()
    val levelProgress = MutableLiveData<LevelProgress>()
    val levelUpEvent = MutableLiveData<LevelProgress?>()
    val deletedEvent = MutableLiveData<DeletedHabit?>()

    val searchQuery = MutableLiveData<String>("")
    val selectedCategory = MutableLiveData<String>("All")

    init {
        items.addSource(habitsLd) {
            habits = it
            recompute()
        }
        items.addSource(completionsLd) {
            completions = it
            recompute()
        }
    }

    fun setQuery(q: String) {
        searchQuery.value = q
        recompute()
    }

    fun setCategory(cat: String) {
        selectedCategory.value = cat
        recompute()
    }

    fun refresh() {
        recompute()
    }

    fun clearMilestone() {
        milestone.value = null
    }

    fun clearLevelUp() {
        levelUpEvent.value = null
    }

    private fun recompute() {
        val h = habits ?: return
        val c = completions ?: return
        val today = DateUtil.today()
        val weekday = DateUtil.weekdayIndex(today) // 0=Mon, 6=Sun
        val dayBit = 1 shl weekday

        val byHabit = c.groupBy({ it.habitId }, { it.day })

        // Calculate RPG XP
        var totalXp = 0
        for (habit in h) {
            val days = (byHabit[habit.id] ?: emptyList()).toSet()
            totalXp += days.size * habit.xpReward
            val currentStreak = Streaks.current(days, today)
            totalXp += LevelManager.streakBonus(currentStreak)
        }

        val progress = LevelManager.getProgress(totalXp)
        levelProgress.value = progress

        val ctx = getApplication<Application>()
        val lastLevel = Prefs.lastSeenLevel(ctx)
        if (progress.level > lastLevel) {
            Prefs.setLastSeenLevel(ctx, progress.level)
            levelUpEvent.value = progress
        }

        val query = searchQuery.value?.trim().orEmpty().lowercase()
        val catFilter = selectedCategory.value ?: "All"

        val filtered = h.filter { habit ->
            val matchesSearch = query.isEmpty() || habit.title.lowercase().contains(query)
            val matchesCategory = catFilter == "All" || habit.category.equals(catFilter, ignoreCase = true)
            matchesSearch && matchesCategory
        }

        items.value = filtered.map { habit ->
            val days = (byHabit[habit.id] ?: emptyList()).toSet()
            val done = days.contains(today)
            val isDue = (habit.targetDaysMask and dayBit) != 0
            val streak = Streaks.current(days, today)

            // At risk: streak >= 1, not completed today, or missed yesterday
            val atRisk = streak > 0 && !done

            HabitUi(
                habit = habit,
                completedToday = done,
                streak = streak,
                atRisk = atRisk,
                isDueToday = isDue
            )
        }
    }

    fun toggle(item: HabitUi) {
        viewModelScope.launch {
            val today = DateUtil.today()
            val dao = db.completionDao()
            if (item.completedToday) {
                dao.delete(item.habit.id, today)
            } else {
                dao.insert(CompletionEntity(habitId = item.habit.id, day = today))
                val days = dao.daysFor(item.habit.id).toSet()
                val streak = Streaks.current(days, today)
                if (streak == 3 || streak == 7 || streak == 14 || streak == 30 || streak == 100) {
                    milestone.value = streak
                }
            }
        }
    }

    fun addHabit(
        title: String,
        emoji: String,
        color: Int,
        category: String,
        targetDaysMask: Int,
        xpReward: Int,
        reminderEnabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            val ctx: Application = getApplication()
            val habit = HabitEntity(
                title = title,
                emoji = emoji,
                color = color,
                category = category,
                targetDaysMask = targetDaysMask,
                xpReward = xpReward,
                reminderEnabled = reminderEnabled,
                reminderHour = hour,
                reminderMinute = minute,
                createdDay = DateUtil.today()
            )
            val id = db.habitDao().insert(habit)
            if (reminderEnabled && Prefs.remindersEnabled(ctx)) {
                ReminderScheduler.schedule(ctx, habit.copy(id = id))
            }
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val ctx: Application = getApplication()
            db.habitDao().update(habit)
            ReminderScheduler.cancel(ctx, habit.id)
            if (habit.reminderEnabled && Prefs.remindersEnabled(ctx)) {
                ReminderScheduler.schedule(ctx, habit)
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val ctx: Application = getApplication()
            val days = db.completionDao().daysFor(habit.id)
            ReminderScheduler.cancel(ctx, habit.id)
            db.habitDao().delete(habit)
            deletedEvent.value = DeletedHabit(habit, days)
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            val ctx: Application = getApplication()
            ReminderScheduler.cancel(ctx, habitId)
            db.habitDao().archive(habitId)
        }
    }

    fun restoreHabit(deleted: DeletedHabit) {
        viewModelScope.launch {
            val ctx: Application = getApplication()
            val newId = db.habitDao().insert(deleted.habit.copy(id = 0))
            for (day in deleted.days) {
                db.completionDao().insert(CompletionEntity(habitId = newId, day = day))
            }
            if (deleted.habit.reminderEnabled && Prefs.remindersEnabled(ctx)) {
                ReminderScheduler.schedule(ctx, deleted.habit.copy(id = newId))
            }
        }
    }

    fun clearDeleted() {
        deletedEvent.value = null
    }

    fun freeze(habitId: Long) {
        viewModelScope.launch {
            db.completionDao().insert(
                CompletionEntity(habitId = habitId, day = DateUtil.today() - 1)
            )
        }
    }
}
