package com.example.poker.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayersReference

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DateListReference

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @PlayersReference
    fun providePlayersReference(firebaseDatabase: FirebaseDatabase): DatabaseReference {
        return firebaseDatabase.getReference("PlayersList")
    }

    @Provides
    @DateListReference
    fun provideDateListReference(firebaseDatabase: FirebaseDatabase): DatabaseReference {
        return firebaseDatabase.getReference("dateList")
    }
}