package com.example.poker.data

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

class StartGameViewModel: ViewModel() {

    // Set the default of row to be 4
    var numOfRows = mutableIntStateOf(4)

    // Define array of 9 player name
    var nameOfPlayerArray = Array(9) { mutableStateOf("") }

    // Define array of 9 how much chip player buy
    var buyMoneyArray = Array(9) { mutableStateOf("")}

    // Define array of 9 how much chip player return in the end of the game
    var returnMoneyArray = Array(9) { mutableStateOf("") }

    //List of all the player that have user in the app
    var playerList: MutableList<String> = mutableListOf()

    // Reference to the data base
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")

    //List of all the player that chosen in this current game
    private var playerListChosen: MutableList<String> = mutableListOf()

    // Massage to pup up in the screen
    var massageDialog =  mutableStateOf<String?>(null)

    // Define the status of loading image to false
    var loading = mutableStateOf(false)

    /**
     * Function to add new player to the list of chosen player
     */
    fun addPlayerToList(name : String){

        // Remove from the list of all available player
        playerList.remove(name)
        playerListChosen.add(name)
    }

    /**
     * Function to remove player from chosen list
     */
    fun removePlayerFromList(name : String){

        // Add to the list of all available player
        playerList.add(name)
        playerListChosen.remove(name)
    }

    /**
     * Function to summarize the transfer log and update the database
     */
    fun finishGameButton() : Boolean{

        if (!CheckInput()){
            return false
        }

        // Variable to check if we have extra or deficit money
        var sumMoney = 0
        var balanceAfterGame = Array(numOfRows.intValue){0}

        // For all player calculation the balance in the end of the game
        for (index in 0..<numOfRows.intValue){
            val currPlayerBalance = returnMoneyArray[index].value.toInt() - buyMoneyArray[index].value.toInt()
            balanceAfterGame[index] = currPlayerBalance
            sumMoney+=currPlayerBalance
        }

        // If sum money is negative we have extra money
        if (sumMoney<0){
            massageDialog.value = "You have a extra of ${abs(sumMoney)}sumMoney shekels."
            return false

        // If sum money is positive we have deficit money
        }else if (sumMoney>0){
            massageDialog.value = "You have a deficit of $sumMoney shekels."
            return false

        // If sum money is zero we don't have extra or deficit
        }else {
            updateBalanceInDataBase(balanceAfterGame)
            calcTransferMoney(balanceAfterGame)
        }
        return true
    }

    /**
     * Function to update the balance of money for each player in the game
     */
    private fun updateBalanceInDataBase(balanceAfterGame: Array<Int>) {
        databaseRef.get().addOnSuccessListener { snapshot ->
            // Go over all the player in the data base
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                var playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)

                // Check if the player play in the current game and the he's balance the name is correct
                if (playerListChosen.contains(playerName) && playerBalance != null &&playerName != null) {
                    val playerIndex = nameOfPlayerArray.indexOfFirst { it.value == playerName }
                    if (playerIndex != -1) {

                        //calc the current balance of money
                        playerBalance += balanceAfterGame[playerIndex]

                        // Update the balance in data base
                        playerSnapshot.ref.child("balance").setValue(playerBalance)
                    }
                }
            }
        }
    }

    /**
     * Function to calculation the transfer money between player
     */
    private fun calcTransferMoney(balanceAfterGame: Array<Int>) {
        val gainers = mutableListOf<Pair<String, Int>>()
        val losers = mutableListOf<Pair<String, Int>>()

        // Split players into gainers and losers
        for (index in balanceAfterGame.indices) {
            val balance = balanceAfterGame[index]
            val playerName = nameOfPlayerArray[index].value

            if (balance > 0) {
                gainers.add(Pair(playerName, balance))
            } else if (balance < 0) {
                losers.add(Pair(playerName, -balance))
            }
        }

        gainers.sortByDescending { it.second }
        losers.sortByDescending { it.second }

        // Run until all the money transfer
        while (gainers.isNotEmpty() && losers.isNotEmpty()){

            //Take the bigger gainers and losers
            val gainPlayer = gainers[0]
            val loserPlayer = losers[0]

            // Calculation how much money transfer between this player
            val transferAmount = minOf(gainPlayer.second,loserPlayer.second)

            // If the gain player receive all the money he need to receive remove form gain list
            if (transferAmount==gainPlayer.second){
                gainers.removeAt(0)

            // Update gain player balance after this transfer
            }else if (transferAmount<gainPlayer.second){
                gainers[0] = gainPlayer.copy(second = gainPlayer.second - transferAmount)
                gainers.sortByDescending { it.second }
            }

            // If loser player send all the money that he need to send , remove from loser list
            if (transferAmount==loserPlayer.second){
                losers.removeAt(0)

            // Update loser player balance after this transfer
            }else if (transferAmount<loserPlayer.second){
                losers[0] = loserPlayer.copy(second = loserPlayer.second - transferAmount)
                losers.sortByDescending { it.second }
            }

            // Add the log of transfer to the list
            TransferLogRepository.addLogEntry("${loserPlayer.first} transfer $transferAmount to ${gainPlayer.first}")
        }
    }

    /**
     * Function that check if user enter correct input
     */
    private fun CheckInput(): Boolean {

        // For each player in the game check if name box contain correct name the money box contain only numbers
        for (index in 0..<numOfRows.intValue){
            if (nameOfPlayerArray[index].value==""||buyMoneyArray[index].value==""||returnMoneyArray[index].value==""){
                massageDialog.value = "Please fill all the field."
                return false
            }
            val isValid1 = buyMoneyArray[index].value.all { it.isDigit() }
            val isValid2 = returnMoneyArray[index].value.all { it.isDigit() }
            if (!isValid1||!isValid2) {
                massageDialog.value = "Please enter numbers only."
                return false
            }
        }
        return true
    }

    /**
     * Function to get all the player name from data base
     */
    fun getPlayerListToStartGame(){
        loading.value = true // Set loading to true when starting
        playerList.clear()
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                if (playerName != null && !playerList.contains(playerName) && !playerListChosen.contains(playerName)) {
                    playerList.add(playerName)
                }
            }
            loading.value = false
        }.addOnFailureListener {
            loading.value = false
        }
    }

    /**
     * Function to remove row form the screen
     */
    fun removeRow(index : Int){
        val playerName = nameOfPlayerArray[index].value

        // If the player that chosen in this row not contain in player list add him
        if (!playerList.contains(playerName)) {
            playerList.add(playerName)
        }

        // reset the variable to default value
        nameOfPlayerArray[index]= mutableStateOf("")
        buyMoneyArray[index] =  mutableStateOf("0")
        returnMoneyArray[index] =  mutableStateOf("0")
    }
}