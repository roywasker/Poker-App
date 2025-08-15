package com.example.poker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlayerSessionDao {
    
    @Query("SELECT * FROM player_sessions WHERE id = 0")
    suspend fun getCurrentSessionOnce(): PlayerSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: PlayerSessionEntity)
    
    @Query("DELETE FROM player_sessions WHERE id = 0")
    suspend fun clearSession()
    
    @Query("SELECT COUNT(*) > 0 FROM player_sessions WHERE id = 0")
    suspend fun hasActiveSession(): Boolean
}