package com.example.poker.data.repository

import com.example.poker.data.base.BaseRepository
import com.example.poker.util.Constants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

/**
 * Optimized implementation of GameRepository with batched queries
 * Fixes N+1 query problem for better performance
 */
class GameRepositoryImpl @Inject constructor(
    private val playersRef: DatabaseReference,
    private val dateListRef: DatabaseReference
) : BaseRepository(), GameRepository {
    
    // Cache for frequently accessed data
    private var cachedPlayers: List<String>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 30_000L // 30 seconds cache
    
    override suspend fun getAllDates(): List<Pair<String, String>> {
        return safeFirebaseCall(emptyList()) {
            val snapshot = dateListRef.get().await()
            snapshot.childrenToList { dateSnapshot ->
                val date = dateSnapshot.getStringOrNull("date")
                val key = dateSnapshot.key
                if (date != null && key != null) {
                    date to key
                } else null
            }
        }
    }
    
    override suspend fun getPlayerBalanceByDate(dateKey: String): List<Pair<String, Int>> {
        return safeFirebaseCall(emptyList()) {
            val databaseRefForHistory = dateListRef.child(dateKey).child(Constants.Firebase.PLAYER_BALANCE_CHILD)
            val snapshot = databaseRefForHistory.get().await()
            
            snapshot.childrenToList { playerSnapshot ->
                val playerName = playerSnapshot.getStringOrNull("name")
                val playerBalance = playerSnapshot.getIntOrDefault("balance", 0)
                if (playerName != null) {
                    playerName to playerBalance
                } else null
            }.sortedByDescending { it.second }
        }
    }
    
    override suspend fun getPlayersForStartGame(): List<String> {
        // Use cache if available and not expired
        val cached = cachedPlayers
        val now = System.currentTimeMillis()
        
        if (cached != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            return cached
        }
        
        return safeFirebaseCall(emptyList()) {
            val snapshot = playersRef.get().await()
            val players = snapshot.childrenToList { playerSnapshot ->
                playerSnapshot.getStringOrNull("name")
            }
            
            // Update cache
            cachedPlayers = players
            cacheTimestamp = now
            
            players
        }
    }
    
    override suspend fun updatePlayerBalance(
        balanceAfterGame: Array<Int>,
        nameOfPlayer: Array<String>
    ): Boolean {
        return safeFirebaseCall(false) {
            coroutineScope {
                // Batch read all player data first
                val playersSnapshot = playersRef.get().await()
                val playerDataMap = mutableMapOf<String, DataSnapshot>()
                
                for (playerSnapshot in playersSnapshot.children) {
                    val name = playerSnapshot.getStringOrNull("name")
                    if (name != null) {
                        playerDataMap[name] = playerSnapshot
                    }
                }
                
                // Prepare all updates
                val updates = mutableListOf<Pair<DatabaseReference, Int>>()
                
                for (i in nameOfPlayer.indices) {
                    if (i >= balanceAfterGame.size) break
                    
                    val playerName = nameOfPlayer[i]
                    if (playerName.isBlank()) continue
                    
                    val playerSnapshot = playerDataMap[playerName]
                    if (playerSnapshot != null) {
                        val currentBalance = playerSnapshot.getIntOrDefault("balance", 0)
                        val newBalance = currentBalance + balanceAfterGame[i]
                        val balanceRef = playerSnapshot.ref.child("balance")
                        updates.add(balanceRef to newBalance)
                    }
                }
                
                // Execute all balance updates in parallel
                val updateJobs = updates.map { (ref, balance) ->
                    async { ref.setValue(balance).await() }
                }
                
                // Add history entry
                val historyJob = async {
                    addGameHistory(balanceAfterGame, nameOfPlayer)
                }
                
                // Wait for all operations to complete
                updateJobs.awaitAll()
                historyJob.await()
                
                true
            }
        }
    }
    
    /**
     * Add game history entry
     */
    private suspend fun addGameHistory(
        balanceAfterGame: Array<Int>,
        nameOfPlayer: Array<String>
    ) {
        val newDateRef = dateListRef.push()
        val dateId = newDateRef.key ?: return
        
        // Create date entry
        val dateData = mapOf(
            "date" to LocalDate.now().toString()
        )
        dateListRef.child(dateId).setValue(dateData).await()
        
        // Add player balances
        val playerBalanceRef = dateListRef.child(dateId).child(Constants.Firebase.PLAYER_BALANCE_CHILD)
        
        for (i in nameOfPlayer.indices) {
            if (i >= balanceAfterGame.size) break
            
            val playerName = nameOfPlayer[i]
            if (playerName.isNotBlank()) {
                val playerData = mapOf(
                    "name" to playerName,
                    "balance" to balanceAfterGame[i]
                )
                
                val playerRef = playerBalanceRef.push()
                playerRef.key?.let { playerId ->
                    playerBalanceRef.child(playerId).setValue(playerData).await()
                }
            }
        }
    }
    
    override suspend fun getPlayerStatistics(playerName: String): PlayerStatistics? {
        return safeFirebaseCall(null) {
            // Batch fetch all required data
            val playersSnapshot = playersRef.get().await()
            val datesSnapshot = dateListRef.get().await()
            
            // Find player data
            var currentBalance = 0
            for (playerSnapshot in playersSnapshot.children) {
                if (playerSnapshot.getStringOrNull("name") == playerName) {
                    currentBalance = playerSnapshot.getIntOrDefault("balance", 0)
                    break
                }
            }
            
            // Process historical data
            val historicalData = processPlayerHistory(playerName, datesSnapshot)
            
            // Calculate statistics
            calculatePlayerStatistics(playerName, currentBalance, historicalData)
        }
    }
    
    override suspend fun getAllPlayersStatistics(): List<PlayerStatistics> {
        return safeFirebaseCall(emptyList()) {
            coroutineScope {
                // Single batch read for all data
                val playersDeferred = async { playersRef.get().await() }
                val datesDeferred = async { dateListRef.get().await() }
                
                val playersSnapshot = playersDeferred.await()
                val datesSnapshot = datesDeferred.await()
                
                // Process all player statistics in parallel
                val statsJobs = playersSnapshot.children.mapNotNull { playerSnapshot ->
                    val playerName = playerSnapshot.getStringOrNull("name")
                    val currentBalance = playerSnapshot.getIntOrDefault("balance", 0)
                    
                    if (playerName != null) {
                        async {
                            val historicalData = processPlayerHistory(playerName, datesSnapshot)
                            calculatePlayerStatistics(playerName, currentBalance, historicalData)
                        }
                    } else null
                }
                
                statsJobs.awaitAll()
                    .filterNotNull()
                    .sortedByDescending { it.currentBalance }
            }
        }
    }
    
    override suspend fun getPlayerHistoricalData(playerName: String): List<Pair<String, Int>> {
        return safeFirebaseCall(emptyList()) {
            val snapshot = dateListRef.get().await()
            processPlayerHistory(playerName, snapshot)
        }
    }
    
    /**
     * Process player history from dates snapshot
     */
    private fun processPlayerHistory(
        playerName: String,
        datesSnapshot: DataSnapshot
    ): List<Pair<String, Int>> {
        val historicalData = mutableListOf<Pair<String, Int>>()
        
        for (dateSnapshot in datesSnapshot.children) {
            val date = dateSnapshot.getStringOrNull("date") ?: continue
            val playerBalanceSnapshot = dateSnapshot.child(Constants.Firebase.PLAYER_BALANCE_CHILD)
            
            for (balanceSnapshot in playerBalanceSnapshot.children) {
                val name = balanceSnapshot.getStringOrNull("name")
                val balance = balanceSnapshot.getIntOrDefault("balance", 0)
                
                if (name == playerName) {
                    historicalData.add(date to balance)
                    break
                }
            }
        }
        
        return historicalData.sortedBy { it.first }
    }
    
    /**
     * Calculate player statistics from historical data
     */
    private fun calculatePlayerStatistics(
        playerName: String,
        currentBalance: Int,
        historicalData: List<Pair<String, Int>>
    ): PlayerStatistics {
        val totalGames = historicalData.size
        val totalWinnings = historicalData.sumOf { it.second }
        val gamesWon = historicalData.count { it.second > 0 }
        val gamesLost = historicalData.count { it.second < 0 }
        val bestGame = historicalData.maxOfOrNull { it.second } ?: 0
        val worstGame = historicalData.minOfOrNull { it.second } ?: 0
        val averageWinLoss = if (totalGames > 0) totalWinnings.toDouble() / totalGames else 0.0
        
        // Calculate streaks
        var currentStreak = 0
        var longestWinStreak = 0
        var longestLossStreak = 0
        var currentWinStreak = 0
        var currentLossStreak = 0
        
        for (result in historicalData.map { it.second }) {
            when {
                result > 0 -> {
                    currentWinStreak++
                    currentLossStreak = 0
                    currentStreak = if (currentStreak >= 0) currentStreak + 1 else 1
                    longestWinStreak = maxOf(longestWinStreak, currentWinStreak)
                }
                result < 0 -> {
                    currentLossStreak++
                    currentWinStreak = 0
                    currentStreak = if (currentStreak <= 0) currentStreak - 1 else -1
                    longestLossStreak = maxOf(longestLossStreak, currentLossStreak)
                }
                else -> {
                    // Draw - doesn't break streak
                }
            }
        }
        
        return PlayerStatistics(
            playerName = playerName,
            totalGames = totalGames,
            gamesWon = gamesWon,
            gamesLost = gamesLost,
            totalWinnings = totalWinnings,
            currentBalance = currentBalance,
            averageWinLoss = averageWinLoss,
            bestGame = bestGame,
            worstGame = worstGame,
            currentStreak = currentStreak,
            longestWinStreak = longestWinStreak,
            longestLossStreak = longestLossStreak
        )
    }
    
    /**
     * Clear cache when needed
     */
    fun clearCache() {
        cachedPlayers = null
        cacheTimestamp = 0
    }
}