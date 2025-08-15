package com.example.poker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlayerSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PokerDatabase : RoomDatabase() {
    
    abstract fun playerSessionDao(): PlayerSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: PokerDatabase? = null
        
        fun getDatabase(context: Context): PokerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PokerDatabase::class.java,
                    "poker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}