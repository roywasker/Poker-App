package com.example.poker.data.repository

import com.example.poker.di.PlayersReference
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    @param:PlayersReference private val playersRef: DatabaseReference
) : PlayerRepository {
    
    override suspend fun getAllPlayers(): List<Pair<String, Int>> {
        val snapshot = playersRef.get().await()
        val playerList = mutableListOf<Pair<String, Int>>()
        
        for (playerSnapshot in snapshot.children) {
            val playerName = playerSnapshot.child("name").getValue(String::class.java)
            val playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)
            if (playerName != null && playerBalance != null) {
                playerList.add(Pair(playerName, playerBalance))
            }
        }
        
        return playerList.sortedByDescending { it.second }
    }
}