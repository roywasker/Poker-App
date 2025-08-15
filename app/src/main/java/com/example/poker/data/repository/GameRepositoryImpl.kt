package com.example.poker.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.example.poker.di.DateListReference
import com.example.poker.di.PlayersReference
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    @param:PlayersReference private val playersRef: DatabaseReference,
    @param:DateListReference private val dateListRef: DatabaseReference
) : GameRepository {
    
    override suspend fun getAllDates(): List<Pair<String, String>> {
        val snapshot = dateListRef.get().await()
        val dateList = mutableListOf<Pair<String, String>>()
        
        for (playerSnapshot in snapshot.children) {
            val date = playerSnapshot.child("date").getValue(String::class.java)
            if (date != null) {
                dateList.add(Pair(date, playerSnapshot.key.toString()))
            }
        }
        
        return dateList
    }
    
    override suspend fun getPlayerBalanceByDate(dateKey: String): List<Pair<String, Int>> {
        val databaseRefForHistory = dateListRef.child(dateKey).child("playerBalance")
        val snapshot = databaseRefForHistory.get().await()
        
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
    
    override suspend fun getPlayersForStartGame(): List<String> {
        val snapshot = playersRef.get().await()
        val playerList = mutableListOf<String>()
        
        for (playerSnapshot in snapshot.children) {
            val playerName = playerSnapshot.child("name").getValue(String::class.java)
            if (playerName != null) {
                playerList.add(playerName)
            }
        }
        
        return playerList
    }
    
    override suspend fun updatePlayerBalance(
        balanceAfterGame: Array<Int>, 
        nameOfPlayer: Array<String>
    ): Boolean {
        return try {
            val currentDate = LocalDate.now()
            
            // Update individual player balances
            val snapshot = playersRef.get().await()
            val playerListChosen = nameOfPlayer.filter { it.isNotEmpty() }
            
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                var playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)
                
                if (playerListChosen.contains(playerName) && playerBalance != null && playerName != null) {
                    val playerIndex = nameOfPlayer.indexOfFirst { it == playerName }
                    if (playerIndex != -1 && playerIndex < balanceAfterGame.size) {
                        playerBalance += balanceAfterGame[playerIndex]
                        playerSnapshot.ref.child("balance").setValue(playerBalance).await()
                    }
                }
            }
            
            // Add history by date to database
            val dateId = dateListRef.push().key
            if (dateId != null) {
                val dateToDB = mapOf("date" to currentDate.toString())
                dateListRef.child(dateId).setValue(dateToDB).await()
                
                val databaseCurrentDateRef = dateListRef.child(dateId).child("playerBalance")
                
                for (index in balanceAfterGame.indices) {
                    val balance = balanceAfterGame[index]
                    val playerName = nameOfPlayer[index]
                    if (playerName.isNotEmpty()) {
                        val playerId = databaseCurrentDateRef.push().key
                        val player = mapOf("name" to playerName, "balance" to balance)
                        if (playerId != null) {
                            databaseCurrentDateRef.child(playerId).setValue(player).await()
                        }
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "updatePlayerBalance: $e")
            false
        }
    }
}