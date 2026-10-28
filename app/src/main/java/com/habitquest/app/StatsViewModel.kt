package com.habitquest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import com.habitquest.app.data.AppDatabase
import com.habitquest.app.data.CompletionEntity
import com.habitquest.app.data.HabitEntity
import com.habitquest.app.rpg.Achievement
import com.habitquest.app.rpg.AchievementsManager
import com.habitquest.app.rpg.LevelManager
import com.habitquest.app.rpg.LevelProgress
import com.habitquest.app.util.DateUtil
import com.habitquest.app.util.Prefs
import com.habitquest.app.util.Streaks

data class HabitBreakdown(
    val habit: HabitEntity,
    val streak: Int,
    val totalDone: Int,
    val rate30: Int
)

data class StatsUi(
    val current: Int,
    val best: Int,
    val counts: Map<Long, Int>,
    val habitCount: Int,
    val weekly: List<Int>,
    val labels: List<String>,
    val average: Int,
    val today: Long,
    val totalCheckIns: Int,
    val rate30: Int,
    val levelProgress: LevelProgress,
    val breakdowns: List<HabitBreakdown>,
    val achievements: List<Achievement>
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private var habits: List<HabitEntity>? = null
    private var completions: List<CompletionEntity>? = null

    val stats = MediatorLiveData<StatsUi>()

    init {
        stats.addSource(db.habitDao().observeAll()) {
            habits = it
            recompute()
        }
        stats.addSource(db.completionDao().observeAll()) {
            completions = it
            recompute()
        }
    }

    fun refresh() {
        recompute()
    }

    private fun recompute() {
        val h = habits ?: return
        val c = completions ?: return
        val today = DateUtil.today()
        val days = c.map { it.day }.toSet()
        val counts: Map<Long, Int> = c.groupingBy { it.day }.eachCount()
        val byHabit = c.groupBy({ it.habitId }, { it.day })

        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        val weekly = ArrayList<Int>()
        val labels = ArrayList<String>()

        for (i in 6 downTo 0) {
            val d = today - i
            val total = h.count { it.createdDay <= d }
            val done = counts[d] ?: 0
            val pct = if (total == 0) 0 else minOf(100, done * 100 / total)
            weekly.add(pct)
            labels.add(dayLetters[DateUtil.weekdayIndex(d)])
        }

        val avg = if (weekly.isEmpty()) 0 else weekly.sum() / weekly.size

        var possible = 0
        var doneIn30 = 0
        for (i in 0 until 30) {
            val d = today - i
            possible += h.count { it.createdDay <= d }
            doneIn30 += counts[d] ?: 0
        }
        val rate30 = if (possible == 0) 0 else minOf(100, doneIn30 * 100 / possible)

        var totalXp = 0
        val breakdowns = mutableListOf<HabitBreakdown>()
        var todayDoneCount = 0

        for (habit in h) {
            val habitDays = (byHabit[habit.id] ?: emptyList()).toSet()
            val streak = Streaks.current(habitDays, today)
            totalXp += habitDays.size * habit.xpReward
            totalXp += LevelManager.streakBonus(streak)
            if (habitDays.contains(today)) todayDoneCount++

            var done30 = 0
            for (i in 0 until 30) {
                if (habitDays.contains(today - i)) done30++
            }
            val hRate30 = minOf(100, done30 * 100 / 30)
            breakdowns.add(
                HabitBreakdown(
                    habit = habit,
                    streak = streak,
                    totalDone = habitDays.size,
                    rate30 = hRate30
                )
            )
        }

        val bestStreak = Streaks.best(days)
        val freezeUsed = Prefs.lastFreezePromptDay(getApplication()) > 0

        val achievements = AchievementsManager.getAchievements(
            totalCompletions = c.size,
            bestStreak = bestStreak,
            habitCount = h.size,
            freezeUsed = freezeUsed,
            completedTodayCount = todayDoneCount
        )

        // Add XP from unlocked achievements
        val achievementXp = achievements.filter { it.isUnlocked }.sumOf { it.xpReward }
        totalXp += achievementXp

        val levelProgress = LevelManager.getProgress(totalXp)

        stats.value = StatsUi(
            current = Streaks.current(days, today),
            best = bestStreak,
            counts = counts,
            habitCount = h.size,
            weekly = weekly,
            labels = labels,
            average = avg,
            today = today,
            totalCheckIns = c.size,
            rate30 = rate30,
            levelProgress = levelProgress,
            breakdowns = breakdowns.sortedByDescending { it.streak },
            achievements = achievements
        )
    }
}
