package com.example.poker.data.repository

interface GameRepository {
    suspend fun getAllDates(): List<Pair<String, String>>
    suspend fun getPlayerBalanceByDate(dateKey: String): List<Pair<String, Int>>
    suspend fun getPlayersForStartGame(): List<String>
    suspend fun updatePlayerBalance(balanceAfterGame: Array<Int>, nameOfPlayer: Array<String>): Boolean
}