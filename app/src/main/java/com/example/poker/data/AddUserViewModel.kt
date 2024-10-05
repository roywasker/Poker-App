package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class AddUserViewModel : ViewModel() {

    var userName = mutableStateOf("")

    //List of all the player that have user in the app
    private var playerList: MutableList<String> = mutableListOf()

    // Reference to the data base
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")

    // Massage to pup up in the screen
    var massageDialog =  mutableStateOf<String?>(null)


    /**
     * Function to save the user name that user enter
     */
    fun onUserNameChange(newName: String) {
        userName.value = newName
    }


    /**
     *  Function to get all the user name that registered in the data base
     */
    fun getPlayerList(){
        playerList.clear()
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) { // Go over all user in the data base
                val playerName = playerSnapshot.child("name").getValue(String::class.java)

                // Add the player name to the list if he don't already in the list
                if (playerName != null && !playerList.contains(playerName)) {
                    playerList.add(playerName)
                }
            }
        }
    }

    /**
     * Function to add new user to the database
     */
    fun addUser() {

        // If player is already in the list pup a massage
        if (playerList.contains(userName.value)){
            massageDialog.value = "Player ${userName.value} is already in the list."
            return;
        }

        // If user try to enter empty name to the list pup a massage
        if (userName.value==""){
            massageDialog.value = "Please enter a name."
            return;
        }


        val playerId = databaseRef.push().key

        // Add to database the player name and set hes balance to zero
        val player = mapOf("name" to userName.value, "balance" to 0)
        if (playerId != null) {
            databaseRef.child(playerId).setValue(player).addOnSuccessListener {
                getPlayerList() // Refresh the player list after adding a new user
                massageDialog.value = "Player ${userName.value} added to the list."
                userName.value="" // clear user name box
            }.addOnFailureListener { e ->
                Log.e("AddUserViewModel", "Failed to add user", e)
                massageDialog.value = "Failed to add player ${userName.value}\n Try later"
            }
        }
    }
}