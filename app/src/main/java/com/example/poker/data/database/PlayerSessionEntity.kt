package com.example.poker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_sessions")
data class PlayerSessionEntity(
    @PrimaryKey val id: Int = 0, // Single session, always ID 0
    val sessionId: String,
    val numOfRows: Int,
    val playerNames: String, // JSON string of player names
    val buyAmounts: String, // JSON string of buy amounts
    val returnAmounts: String // JSON string of return amounts
)