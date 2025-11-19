package com.example.emoto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(mood: Mood)

    @Query("SELECT * FROM mood_table ORDER BY timestamp DESC")
    suspend fun getAllMoods(): List<Mood>
}
