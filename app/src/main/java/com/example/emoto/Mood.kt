package com.example.emoto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_table")
data class Mood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emoji: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
