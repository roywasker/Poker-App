package com.example.poker.di

import android.content.Context
import androidx.room.Room
import com.example.poker.data.database.PlayerSessionDao
import com.example.poker.data.database.PlayerSessionRepository
import com.example.poker.data.database.PokerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePokerDatabase(@ApplicationContext context: Context): PokerDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PokerDatabase::class.java,
            "poker_database"
        ).build()
    }

    @Provides
    fun providePlayerSessionDao(database: PokerDatabase): PlayerSessionDao {
        return database.playerSessionDao()
    }
    
    @Provides
    @Singleton
    fun providePlayerSessionRepository(dao: PlayerSessionDao): PlayerSessionRepository {
        return PlayerSessionRepository(dao)
    }
}