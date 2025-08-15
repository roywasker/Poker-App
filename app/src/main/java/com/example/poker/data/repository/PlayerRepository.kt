package com.example.poker.data.repository

interface PlayerRepository {
    suspend fun getAllPlayers(): List<Pair<String, Int>>
}