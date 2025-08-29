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
    
    override suspend fun getPlayerHistoricalData(playerName: String): List<Pair<String, Int>> {
        val snapshot = dateListRef.get().await()
        val playerHistory = mutableListOf<Pair<String, Int>>()
        
        for (dateSnapshot in snapshot.children) {
            val date = dateSnapshot.child("date").getValue(String::class.java)
            val playerBalanceSnapshot = dateSnapshot.child("playerBalance")
            
            for (playerSnapshot in playerBalanceSnapshot.children) {
                val name = playerSnapshot.child("name").getValue(String::class.java)
                val balance = playerSnapshot.child("balance").getValue(Int::class.java)
                
                if (name == playerName && date != null && balance != null) {
                    playerHistory.add(Pair(date, balance))
                }
            }
        }
        
        return playerHistory.sortedBy { it.first }
    }
    
    override suspend fun getPlayerStatistics(playerName: String): PlayerStatistics? {
        return try {
            // Get current balance from players reference
            val playerSnapshot = playersRef.get().await()
            var currentBalance = 0
            
            for (player in playerSnapshot.children) {
                val name = player.child("name").getValue(String::class.java)
                if (name == playerName) {
                    currentBalance = player.child("balance").getValue(Int::class.java) ?: 0
                    break
                }
            }
            
            // Get historical data
            val historicalData = getPlayerHistoricalData(playerName)
            
            if (historicalData.isEmpty()) {
                return PlayerStatistics(
                    playerName = playerName,
                    totalGames = 0,
                    gamesWon = 0,
                    gamesLost = 0,
                    totalWinnings = currentBalance,
                    currentBalance = currentBalance,
                    averageWinLoss = 0.0,
                    bestGame = 0,
                    worstGame = 0,
                    currentStreak = 0,
                    longestWinStreak = 0,
                    longestLossStreak = 0
                )
            }
            
            val totalGames = historicalData.size
            val gamesWon = historicalData.count { it.second > 0 }
            val gamesLost = historicalData.count { it.second < 0 }
            val totalWinnings = historicalData.sumOf { it.second }
            val averageWinLoss = if (totalGames > 0) totalWinnings.toDouble() / totalGames else 0.0
            val bestGame = historicalData.maxOfOrNull { it.second } ?: 0
            val worstGame = historicalData.minOfOrNull { it.second } ?: 0
            
            // Calculate streaks
            val streaks = calculateStreaks(historicalData.map { it.second })
            
            PlayerStatistics(
                playerName = playerName,
                totalGames = totalGames,
                gamesWon = gamesWon,
                gamesLost = gamesLost,
                totalWinnings = totalWinnings,
                currentBalance = currentBalance,
                averageWinLoss = averageWinLoss,
                bestGame = bestGame,
                worstGame = worstGame,
                currentStreak = streaks.currentStreak,
                longestWinStreak = streaks.longestWinStreak,
                longestLossStreak = streaks.longestLossStreak
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "getPlayerStatistics: $e")
            null
        }
    }
    
    override suspend fun getAllPlayersStatistics(): List<PlayerStatistics> {
        return try {
            val players = getPlayersForStartGame()
            val allStats = mutableListOf<PlayerStatistics>()
            
            for (player in players) {
                getPlayerStatistics(player)?.let { stats ->
                    allStats.add(stats)
                }
            }
            
            allStats.sortedByDescending { it.currentBalance }
        } catch (e: Exception) {
            Log.e(TAG, "getAllPlayersStatistics: $e")
            emptyList()
        }
    }
    
    private data class StreakData(
        val currentStreak: Int,
        val longestWinStreak: Int,
        val longestLossStreak: Int
    )
    
    private fun calculateStreaks(gameResults: List<Int>): StreakData {
        if (gameResults.isEmpty()) {
            return StreakData(0, 0, 0)
        }
        
        var currentStreak = 0
        var longestWinStreak = 0
        var longestLossStreak = 0
        var currentWinStreak = 0
        var currentLossStreak = 0
        
        for (result in gameResults) {
            when {
                result > 0 -> {
                    if (currentStreak >= 0) {
                        currentStreak++
                    } else {
                        currentStreak = 1
                    }
                    currentWinStreak++
                    longestWinStreak = maxOf(longestWinStreak, currentWinStreak)
                    currentLossStreak = 0
                }
                result < 0 -> {
                    if (currentStreak <= 0) {
                        currentStreak--
                    } else {
                        currentStreak = -1
                    }
                    currentLossStreak++
                    longestLossStreak = maxOf(longestLossStreak, currentLossStreak)
                    currentWinStreak = 0
                }
                else -> {
                    // Tie game - doesn't affect streaks
                }
            }
        }
        
        return StreakData(currentStreak, longestWinStreak, longestLossStreak)
    }
}