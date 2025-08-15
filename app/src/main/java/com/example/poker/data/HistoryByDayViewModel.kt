package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class HistoryByDayViewModel : ViewModel() {

    //List of all the player that have user in the app
    var dateList:  MutableList<Pair<String, String>> = mutableListOf()

    // Reference to the data base
    private var databaseRef = FirebaseDatabase.getInstance().getReference("dateList")

    // Player list
    val playerList: MutableList<Pair<String, Int>> = mutableListOf()

    // Define the status of loading image to false
    var loading = mutableStateOf(false)

    //data that your pick
    var dateSelected = mutableStateOf<Pair<String, String>>(Pair("Pick a date", ""))

    // Message to popup in the screen
    var messageDialog =  mutableStateOf<String?>(null)

    /**
     *  Function to get all date of game
     */
    fun getDateList(){
        loading.value = true
        dateList.clear()
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) { // Go over all date in the data base
                val date = playerSnapshot.child("date").getValue(String::class.java)

                // Add the date to the list
                if (date != null) {
                    dateList.add(Pair(date,playerSnapshot.key.toString()))
                }
            }
        }
        loading.value = false
    }

    /**
     * Function to get all player balance by this date
     */
    fun getPlayerBalanceByDate(){
        if (dateSelected.value.first == "Pick a date"){
            messageDialog.value = "Please pick a date"
            return
        }
        loading.value = true // Set loading to true when starting
        playerList.clear()

        val databaseRefForHistory = databaseRef.child(dateSelected.value.second).child("playerBalance")
        databaseRefForHistory.get().addOnSuccessListener { snapshot ->
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