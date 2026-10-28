package com.habitquest.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CompletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity): Long

    @Query("DELETE FROM completions WHERE habitId = :habitId AND day = :day")
    suspend fun delete(habitId: Long, day: Long)

    @Query("SELECT * FROM completions")
    fun observeAll(): LiveData<List<CompletionEntity>>

    @Query("SELECT day FROM completions WHERE habitId = :habitId ORDER BY day ASC")
    suspend fun daysFor(habitId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM completions WHERE habitId = :habitId AND day = :day")
    suspend fun countFor(habitId: Long, day: Long): Int

    @Query("SELECT COUNT(*) FROM completions")
    suspend fun totalCompletions(): Int

    @Query("SELECT * FROM completions")
    suspend fun getAll(): List<CompletionEntity>
}
