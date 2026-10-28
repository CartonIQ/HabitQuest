package com.habitquest.app.util

object Streaks {
    /** Consecutive days ending today (or yesterday if today is not done yet). */
    fun current(days: Set<Long>, today: Long): Int {
        var d = if (days.contains(today)) today else today - 1
        var count = 0
        while (days.contains(d)) {
            count++
            d--
        }
        return count
    }

    fun best(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                run++
            } else {
                run = 1
            }
            if (run > best) best = run
        }
        return best
    }
}
