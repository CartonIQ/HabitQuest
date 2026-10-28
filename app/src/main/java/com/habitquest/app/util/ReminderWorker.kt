package com.habitquest.app.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.habitquest.app.MainActivity
import com.habitquest.app.R
import com.habitquest.app.data.AppDatabase
import com.habitquest.app.data.HabitEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!Prefs.remindersEnabled(ctx)) return Result.success()
        val habitId = inputData.getLong(KEY_HABIT_ID, -1L)
        if (habitId < 0L) return Result.success()

        val db = AppDatabase.get(ctx)
        val habit = db.habitDao().getById(habitId) ?: return Result.success()
        val doneToday = db.completionDao().countFor(habitId, DateUtil.today()) > 0
        if (!doneToday) {
            notifyUser(ctx, habit)
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notifyUser(ctx: Context, habit: HabitEntity) {
        ReminderScheduler.ensureChannel(ctx)
        val manager = NotificationManagerCompat.from(ctx)
        if (!manager.areNotificationsEnabled()) return

        val intent = Intent(ctx, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            ctx,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_leaf)
            .setContentTitle(habit.emoji + " " + habit.title)
            .setContentText("Time for your habit. Keep your streak going!")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(habit.id.toInt(), notification)
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "habit_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Habit reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Daily nudges for your habits"
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun workName(habitId: Long) = "habit_reminder_$habitId"

    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    fun schedule(context: Context, habit: HabitEntity) {
        if (!habit.reminderEnabled) return
        ensureChannel(context)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(habit.reminderHour, habit.reminderMinute), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_HABIT_ID to habit.id))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(workName(habit.id), ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context, habitId: Long) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(habitId))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelAllWork()
    }

    suspend fun rescheduleAll(context: Context) {
        val habits = AppDatabase.get(context).habitDao().getAll()
        for (habit in habits) {
            if (habit.reminderEnabled) schedule(context, habit)
        }
    }
}
