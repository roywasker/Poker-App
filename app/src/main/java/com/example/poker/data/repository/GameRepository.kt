package com.example.poker.data.repository

data class PlayerStatistics(
    val playerName: String,
    val totalGames: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val totalWinnings: Int,
    val currentBalance: Int,
    val averageWinLoss: Double,
    val bestGame: Int,
    val worstGame: Int,
    val currentStreak: Int,
    val longestWinStreak: Int,
    val longestLossStreak: Int
)

interface GameRepository {
    suspend fun getAllDates(): List<Pair<String, String>>
    suspend fun getPlayerBalanceByDate(dateKey: String): List<Pair<String, Int>>
    suspend fun getPlayersForStartGame(): List<String>
    suspend fun updatePlayerBalance(balanceAfterGame: Array<Int>, nameOfPlayer: Array<String>): Boolean
    suspend fun getPlayerStatistics(playerName: String): PlayerStatistics?
    suspend fun getAllPlayersStatistics(): List<PlayerStatistics>
    suspend fun getPlayerHistoricalData(playerName: String): List<Pair<String, Int>>
}