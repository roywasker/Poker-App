package com.example.poker.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class PlayerBalanceViewModel : ViewModel(){
    // Define the status of loading image to false
    var loading = mutableStateOf(false)

    // Reference to the data base
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")

    // Player list
    val playerList: MutableList<Pair<String, Int>> = mutableListOf()

    /**
     *
     */
    fun gerPlayerBalance(){
        loading.value = true // Set loading to true when starting
        playerList.clear()
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                val playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)
                if (playerName != null && playerBalance != null) {
                    playerList.add(Pair(playerName, playerBalance))
                }
            }
            playerList.sortByDescending { it.second }
            loading.value = false
        }.addOnFailureListener {
            loading.value = false
        }
    }
}