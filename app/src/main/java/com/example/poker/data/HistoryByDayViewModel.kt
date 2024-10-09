package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class HistoryByDayViewModel : ViewModel() {

    //List of all the player that have user in the app
    var dateList: MutableList<String> = mutableListOf()

    // Reference to the data base
    private var databaseRef = FirebaseDatabase.getInstance().getReference("dateList")

    // Player list
    val playerList: MutableList<Pair<String, Int>> = mutableListOf()

    // Define the status of loading image to false
    var loading = mutableStateOf(false)

    //data that your pick
    var dateSelected = mutableStateOf("Pick a date")

    // Massage to pup up in the screen
    var massageDialog =  mutableStateOf<String?>(null)

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
                    dateList.add(date)
                }
                //Log.d("getDateList", "Fetched date: $date")
            }
        }
        loading.value = false
    }

    /**
     * Function to get all player balance by this date
     */
    fun gerPlayerBalanceByDate(){
        if (dateSelected.value == "Pick a date"){
            massageDialog.value = "Please pick a date"
            return
        }
        loading.value = true // Set loading to true when starting
        playerList.clear()
        databaseRef = databaseRef.child(dateSelected.value)
        databaseRef.get().addOnSuccessListener { snapshot ->
            Log.d("getDateList", "Fetched player")
            for (playerSnapshot in snapshot.children) {
                Log.d("getDateList", "Fetched player 2")
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                val playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)
                Log.d("getDateList", "Fetched player 3")
                if (playerName != null && playerBalance != null) {
                    playerList.add(Pair(playerName, playerBalance))
                }
                Log.d("getDateList", "Fetched player : $playerName")
            }
            playerList.sortByDescending { it.second }
            loading.value = false
        }.addOnFailureListener {
            loading.value = false
        }
    }
}