package com.habitquest.app.rpg

import kotlin.math.sqrt

data class LevelProgress(
    val level: Int,
    val rankTitle: String,
    val rankEmoji: String,
    val currentXp: Int,
    val currentLevelBaseXp: Int,
    val nextLevelXp: Int,
    val progressPercent: Int
)

object LevelManager {
    const val BASE_XP_PER_HABIT = 25
    const val PERFECT_DAY_BONUS = 50

    private val TITLES = listOf(
        1 to Pair("Novice Seeker", "🌱"),
        3 to Pair("Habit Apprentice", "⚔️"),
        5 to Pair("Focus Warrior", "🛡️"),
        8 to Pair("Steadfast Initiate", "⚡"),
        12 to Pair("Routine Ranger", "🏹"),
        16 to Pair("Discipline Paladin", "✨"),
        20 to Pair("Iron Will Champion", "👑"),
        30 to Pair("Streak Legend", "🔥"),
        40 to Pair("Grand Questmaster", "🐉"),
        50 to Pair("Mythic Sovereign", "🌟")
    )

    /** Calculate XP threshold needed to reach given level (quadratic progression) */
    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        return ((level - 1) * (level - 1) * 75) + ((level - 1) * 100)
    }

    /** Calculate current level from total accumulated XP */
    fun levelForXp(xp: Int): Int {
        if (xp <= 0) return 1
        var lvl = 1
        while (xpForLevel(lvl + 1) <= xp) {
            lvl++
            if (lvl >= 100) break
        }
        return lvl
    }

    /** Calculate bonus XP earned for a streak */
    fun streakBonus(streak: Int): Int {
        return when {
            streak >= 30 -> 100
            streak >= 14 -> 50
            streak >= 7 -> 25
            streak >= 3 -> 10
            else -> 0
        }
    }

    fun getProgress(totalXp: Int): LevelProgress {
        val level = levelForXp(totalXp)
        val currentBase = xpForLevel(level)
        val nextBase = xpForLevel(level + 1)
        val xpInLevel = (totalXp - currentBase).coerceAtLeast(0)
        val span = (nextBase - currentBase).coerceAtLeast(1)
        val pct = ((xpInLevel.toDouble() / span) * 100).toInt().coerceIn(0, 100)

        var title = TITLES[0].second.first
        var emoji = TITLES[0].second.second
        for ((minLvl, pair) in TITLES) {
            if (level >= minLvl) {
                title = pair.first
                emoji = pair.second
            }
        }

        return LevelProgress(
            level = level,
            rankTitle = title,
            rankEmoji = emoji,
            currentXp = totalXp,
            currentLevelBaseXp = currentBase,
            nextLevelXp = nextBase,
            progressPercent = pct
        )
    }
}
