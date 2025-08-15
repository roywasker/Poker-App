package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class AddUserViewModel : ViewModel() {

    var userName = mutableStateOf("")

    //List of all the player that have user in the app
    var playerList: MutableList<String> = mutableListOf()

    // Reference to the data base
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")

    var userToChange = mutableStateOf("")

    var moneyChange = mutableStateOf("")

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
            return
        }

        // If user try to enter empty name to the list pup a massage
        if (userName.value==""){
            massageDialog.value = "Please enter a name."
            return
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

    fun changeUserValue(){
        if (moneyChange.value == "" || userToChange.value == ""){
            massageDialog.value = "Enter all the field "
            return
        }
        if (moneyChange.value.toInt() < 0 ){
            massageDialog.value = "It is not possible to subtract a negative amount from a user."
            return
        }
        Log.d("roy1234", "changeUserValue: ${userToChange.value} ${moneyChange.value}")

        databaseRef.get().addOnSuccessListener { snapshot ->
            // Go over all the player in the data base
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                var playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)

                // Check if the player play in the current game and the he's balance the name is correct
                if (playerName?.let { userToChange.value.contains(it) } == true) {
                    if (playerBalance != null) {
                        playerBalance -= moneyChange.value.toInt()

                        // Update the balance in data base
                        playerSnapshot.ref.child("balance").setValue(playerBalance)
                    }
                }
            }
        }.addOnSuccessListener {
            massageDialog.value = "Reduction successfully completed"
            userToChange.value = ""
            moneyChange.value = ""
        }
    }
}