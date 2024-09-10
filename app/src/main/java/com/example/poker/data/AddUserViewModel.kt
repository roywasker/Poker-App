package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class AddUserViewModel : ViewModel() {

    var userName = mutableStateOf("")
    private var playerList: MutableList<String> = mutableListOf()
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")
    var massageDialog =  mutableStateOf<String?>(null)

    fun onUserNameChange(newName: String) {
        userName.value = newName
    }

    fun getPlayerList(){
        playerList.clear()
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                if (playerName != null && !playerList.contains(playerName)) {
                    playerList.add(playerName)
                    Log.d("AddUserViewModel", playerName)
                }
            }
        }
    }

    fun addUser() {
        if (playerList.contains(userName.value)){
            massageDialog.value = "Player ${userName.value} is already in the list."
            return;
        }
        if (userName.value==""){
            massageDialog.value = "Please enter a name."
            return;
        }
        val playerId = databaseRef.push().key
        val player = mapOf("name" to userName.value, "balance" to 0)
        if (playerId != null) {
            databaseRef.child(playerId).setValue(player).addOnSuccessListener {
                Log.d("AddUserViewModel", "User added successfully")
                getPlayerList() // Refresh the player list after adding a new user
                massageDialog.value = "Player ${userName.value} added to the list."
                userName.value=""
            }.addOnFailureListener { e ->
                Log.e("AddUserViewModel", "Failed to add user", e)
                massageDialog.value = "Failed to add player ${userName.value}\n Try later"
            }
        }
    }
}