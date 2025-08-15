package com.example.poker.data.repository

interface UserRepository {
    suspend fun addUser(userName: String, initialBalance: Int): Boolean
    suspend fun getUserList(): List<String>
    suspend fun changeUserBalance(userName: String, newBalance: Int): Boolean
}