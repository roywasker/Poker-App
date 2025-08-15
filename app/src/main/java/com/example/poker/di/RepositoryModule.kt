package com.example.poker.di

import com.example.poker.data.repository.GameRepository
import com.example.poker.data.repository.GameRepositoryImpl
import com.example.poker.data.repository.PlayerRepository
import com.example.poker.data.repository.PlayerRepositoryImpl
import com.example.poker.data.repository.UserRepository
import com.example.poker.data.repository.UserRepositoryImpl
import com.google.firebase.database.DatabaseReference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePlayerRepository(
        @PlayersReference playersRef: DatabaseReference
    ): PlayerRepository {
        return PlayerRepositoryImpl(playersRef)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        @PlayersReference playersRef: DatabaseReference,
        @DateListReference dateListRef: DatabaseReference
    ): GameRepository {
        return GameRepositoryImpl(playersRef, dateListRef)
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        @PlayersReference playersRef: DatabaseReference
    ): UserRepository {
        return UserRepositoryImpl(playersRef)
    }
}