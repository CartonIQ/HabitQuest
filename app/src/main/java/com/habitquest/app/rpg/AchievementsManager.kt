package com.habitquest.app.rpg

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val xpReward: Int,
    val isUnlocked: Boolean,
    val progress: Int,
    val maxProgress: Int
)

object AchievementsManager {

    fun getAchievements(
        totalCompletions: Int,
        bestStreak: Int,
        habitCount: Int,
        freezeUsed: Boolean,
        completedTodayCount: Int
    ): List<Achievement> {
        return listOf(
            Achievement(
                id = "first_step",
                title = "First Step",
                description = "Complete your very first quest habit",
                iconEmoji = "🌱",
                xpReward = 50,
                isUnlocked = totalCompletions >= 1,
                progress = minOf(totalCompletions, 1),
                maxProgress = 1
            ),
            Achievement(
                id = "spark_3",
                title = "Flame Initiate",
                description = "Maintain a 3-day habit streak",
                iconEmoji = "🔥",
                xpReward = 75,
                isUnlocked = bestStreak >= 3,
                progress = minOf(bestStreak, 3),
                maxProgress = 3
            ),
            Achievement(
                id = "warrior_7",
                title = "Weekly Warrior",
                description = "Maintain a 7-day streak across any habit",
                iconEmoji = "⚔️",
                xpReward = 150,
                isUnlocked = bestStreak >= 7,
                progress = minOf(bestStreak, 7),
                maxProgress = 7
            ),
            Achievement(
                id = "titan_30",
                title = "Iron Discipline",
                description = "Achieve an uninterrupted 30-day streak",
                iconEmoji = "🛡️",
                xpReward = 300,
                isUnlocked = bestStreak >= 30,
                progress = minOf(bestStreak, 30),
                maxProgress = 30
            ),
            Achievement(
                id = "checkins_50",
                title = "Habit Crusader",
                description = "Record 50 total quest check-ins",
                iconEmoji = "⚡",
                xpReward = 200,
                isUnlocked = totalCompletions >= 50,
                progress = minOf(totalCompletions, 50),
                maxProgress = 50
            ),
            Achievement(
                id = "centurion_100",
                title = "Centurion Master",
                description = "Complete 100 total habit check-ins",
                iconEmoji = "👑",
                xpReward = 500,
                isUnlocked = totalCompletions >= 100,
                progress = minOf(totalCompletions, 100),
                maxProgress = 100
            ),
            Achievement(
                id = "perfect_day",
                title = "Flawless Day",
                description = "Fulfill 100% of all scheduled habits for the day",
                iconEmoji = "✨",
                xpReward = 100,
                isUnlocked = habitCount > 0 && completedTodayCount == habitCount,
                progress = if (habitCount > 0 && completedTodayCount == habitCount) 1 else 0,
                maxProgress = 1
            ),
            Achievement(
                id = "freeze_shield",
                title = "Cryo Guardian",
                description = "Use a Streak Freeze to safeguard your discipline",
                iconEmoji = "❄️",
                xpReward = 50,
                isUnlocked = freezeUsed,
                progress = if (freezeUsed) 1 else 0,
                maxProgress = 1
            )
        )
    }
}
