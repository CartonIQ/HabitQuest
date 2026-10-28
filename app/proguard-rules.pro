# HabitQuest ProGuard rules
-keep class com.habitquest.app.data.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
