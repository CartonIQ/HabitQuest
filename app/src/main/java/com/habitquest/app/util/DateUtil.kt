package com.habitquest.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtil {
    private const val DAY_MS = 86_400_000L

    /** Days since 1970-01-01 in the device's local time zone. */
    fun today(): Long {
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        return Math.floorDiv(now + offset, DAY_MS)
    }

    /** Monday = 0 ... Sunday = 6. Epoch day 0 (1970-01-01) was a Thursday. */
    fun weekdayIndex(epochDay: Long): Int = Math.floorMod(epochDay + 3L, 7L).toInt()

    fun formatToday(): String =
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
