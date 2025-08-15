package com.example.poker.data.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.example.poker.di.PlayersReference
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    @param:PlayersReference private val playersRef: DatabaseReference
) : UserRepository {
    
    override suspend fun addUser(userName: String, initialBalance: Int): Boolean {
        return try {
            val snapshot = playersRef.get().await()
            
            // Check if user already exists
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                if (playerName == userName) {
                    return false // User already exists
                }
            }
            
            // Add new user
            val playerId = playersRef.push().key
            if (playerId != null) {
                val player = mapOf(
                    "name" to userName,
                    "balance" to initialBalance
                )
                playersRef.child(playerId).setValue(player).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "addUser: $e")
            false
        }
    }
    
    override suspend fun getUserList(): List<String> {
        return try {
            val snapshot = playersRef.get().await()
            val userList = mutableListOf<String>()
            
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                if (playerName != null) {
                    userList.add(playerName)
                }
            }
            userList
        } catch (e: Exception) {
            Log.e(TAG, "getUserList: $e")
            emptyList()
        }
    }
    
    override suspend fun changeUserBalance(userName: String, newBalance: Int): Boolean {
        return try {
            val snapshot = playersRef.get().await()
            
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                if (playerName == userName) {
                    playerSnapshot.ref.child("balance").setValue(newBalance).await()
                    return true
                }
            }
            false // User not found
        } catch (e: Exception) {
            Log.e(TAG, "changeUserBalance: $e")
            false
        }
    }
}