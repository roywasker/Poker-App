package com.example.poker.data

import android.app.Application
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.data.database.PlayerSessionRepository
import com.example.poker.data.database.PokerDatabase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.abs

class StartGameViewModel(application: Application): AndroidViewModel(application) {

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

    private var databaseDateRef = FirebaseDatabase.getInstance().getReference("dateList")

    //List of all the player that chosen in this current game
    private var playerListChosen: MutableList<String> = mutableListOf()

    // Message to popup in the screen
    var messageDialog =  mutableStateOf<String?>(null)

    // Define the status of loading image to false
    var loading = mutableStateOf(false)

    // Local database repository
    private val sessionRepository: PlayerSessionRepository

    // Track if there's an active session
    var hasActiveSession = mutableStateOf(false)

    init {
        val database = PokerDatabase.getDatabase(application)
        sessionRepository = PlayerSessionRepository(database.playerSessionDao())
        
        // Check for existing session on startup
        viewModelScope.launch {
            hasActiveSession.value = sessionRepository.hasActiveSession()
            if (hasActiveSession.value) {
                loadSavedSession()
            }
        }
    }

    /**
     * Function to add new player to the list of chosen player
     */
    fun addPlayerToList(name : String){

        // Remove from the list of all available player
        playerList.remove(name)
        playerListChosen.add(name)
        
        // Save session after any change
        saveCurrentSession()
    }

    /**
     * Function to remove player from chosen list
     */
    fun removePlayerFromList(name : String){

        // Add to the list of all available player
        playerList.add(name)
        playerListChosen.remove(name)
        
        // Save session after any change
        saveCurrentSession()
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
        val balanceAfterGame = Array(numOfRows.intValue){0}

        // For all player calculation the balance in the end of the game
        for (index in 0..<numOfRows.intValue){
            val currPlayerBalance = returnMoneyArray[index].value.toInt() - buyMoneyArray[index].value.toInt()
            balanceAfterGame[index] = currPlayerBalance
            sumMoney += currPlayerBalance
        }

        // If sum money is negative we have extra money
        if (sumMoney<0){
            messageDialog.value = "You have a extra of ${abs(sumMoney)} shekels."
            return false

        // If sum money is positive we have deficit money
        }else if (sumMoney>0){
            messageDialog.value = "You have a deficit of $sumMoney shekels."
            return false

        // If sum money is zero we don't have extra or deficit
        }else {
            calcTransferMoney(balanceAfterGame)
            updateBalanceInDataBase(balanceAfterGame,nameOfPlayerArray)
        }
        return true
    }

    /**
     * Function to reset the data box in game screen
     */
    private fun resetPageData(){
        buyMoneyArray = Array(9) { mutableStateOf("")}
        returnMoneyArray = Array(9) { mutableStateOf("")}
        playerListChosen.clear()
        numOfRows = mutableIntStateOf(4)
        nameOfPlayerArray = Array(9) { mutableStateOf("") }
        getPlayerListToStartGame()
    }

    /**
     * Function to save current session to local database
     */
    private fun saveCurrentSession() {
        viewModelScope.launch {
            sessionRepository.saveSession(
                numOfRows = numOfRows.intValue,
                playerNames = nameOfPlayerArray,
                buyAmounts = buyMoneyArray,
                returnAmounts = returnMoneyArray
            )
            hasActiveSession.value = true
        }
    }

    /**
     * Function to load saved session from local database
     */
    private suspend fun loadSavedSession() {
        val sessionData = sessionRepository.loadSession()
        sessionData?.let { data ->
            numOfRows.intValue = data.numOfRows
            nameOfPlayerArray = data.playerNames
            buyMoneyArray = data.buyAmounts
            returnMoneyArray = data.returnAmounts
            
            // Rebuild chosen players list
            playerListChosen.clear()
            for (i in 0 until data.numOfRows) {
                val playerName = data.playerNames[i].value
                if (playerName.isNotEmpty()) {
                    playerListChosen.add(playerName)
                }
            }
        }
    }

    /**
     * Function to clear saved session
     */
    fun clearSavedSession() {
        viewModelScope.launch {
            sessionRepository.clearSession()
            hasActiveSession.value = false
            resetPageData()
        }
    }

    /**
     * Function to manually trigger session save (called when user inputs data)
     */
    fun onDataChanged() {
        saveCurrentSession()
    }

    /**
     * Function to update the balance of money for each player in the game
     */
    private fun updateBalanceInDataBase(
        balanceAfterGame: Array<Int>,
        nameOfPlayer: Array<MutableState<String>>
    ){
        viewModelScope.launch {
            try {
                // get the current date
                val currentDate = LocalDate.now()

                val snapshot = withContext(Dispatchers.IO) {
                    databaseRef.get().await()
                }
                
                // Go over all the player in the data base
                for (playerSnapshot in snapshot.children) {
                    val playerName = playerSnapshot.child("name").getValue(String::class.java)
                    var playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)

                    // Check if the player play in the current game and the he's balance the name is correct
                    if (playerListChosen.contains(playerName) && playerBalance != null && playerName != null) {
                        val playerIndex = this@StartGameViewModel.nameOfPlayerArray.indexOfFirst { it.value == playerName }
                        if (playerIndex != -1) {

                            //calc the current balance of money
                            playerBalance += balanceAfterGame[playerIndex]

                            // Update the balance in data base
                            withContext(Dispatchers.IO) {
                                playerSnapshot.ref.child("balance").setValue(playerBalance).await()
                            }
                        }
                    }
                }

                // Add history by date to database
                val dateId = databaseDateRef.push().key
                if (dateId != null){

                    val dateToDB = mapOf("date" to currentDate.toString())
                    // Open new folder for current date
                    withContext(Dispatchers.IO) {
                        databaseDateRef.child(dateId).setValue(dateToDB).await()
                    }
                    
                    val databaseCurrentDateRef = databaseDateRef.child(dateId).child("playerBalance")

                    //add all the user balance of this game
                    for (index in balanceAfterGame.indices) {
                        val balance = balanceAfterGame[index]
                        val playerName = nameOfPlayer[index].value
                        val playerId = databaseCurrentDateRef.push().key
                        val player = mapOf("name" to playerName, "balance" to balance)
                        if (playerId != null) {
                            withContext(Dispatchers.IO) {
                                databaseCurrentDateRef.child(playerId).setValue(player).await()
                            }
                        }
                    }

                    //reset all the array form data
                    resetPageData()
                    
                    // Clear saved session after successful game completion
                    sessionRepository.clearSession()
                    hasActiveSession.value = false
                }
            } catch (e: Exception) {
                // Handle error appropriately
                messageDialog.value = "Failed to update game data. Please try again."
            }
        }
    }

    /**
     * Function to calculation the transfer money between player
     */
    private fun calcTransferMoney(balanceAfterGame: Array<Int>) {
        val gainers = mutableListOf<Pair<String, Int>>()
        val losers = mutableListOf<Pair<String, Int>>()

        //clean the transfer list
        TransferLogRepository.cleanTransferLog()

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
                messageDialog.value = "Please fill all the field."
                return false
            }
            val isValid1 = buyMoneyArray[index].value.all { it.isDigit() }
            val isValid2 = returnMoneyArray[index].value.all { it.isDigit() }
            if (!isValid1||!isValid2) {
                messageDialog.value = "Please enter numbers only."
                return false
            }
        }
        return true
    }

    /**
     * Function to get all the player name from data base
     */
    fun getPlayerListToStartGame(){
        viewModelScope.launch {
            loading.value = true
            playerList.clear()
            
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    databaseRef.get().await()
                }
                
                for (playerSnapshot in snapshot.children) {
                    val playerName = playerSnapshot.child("name").getValue(String::class.java)
                    if (playerName != null && !playerList.contains(playerName) && !playerListChosen.contains(playerName)) {
                        playerList.add(playerName)
                    }
                }
                loading.value = false
            } catch (e: Exception) {
                loading.value = false
                // Handle error appropriately
            }
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
        buyMoneyArray[index] =  mutableStateOf("")
        returnMoneyArray[index] =  mutableStateOf("")
        
        // Save session after row removal
        saveCurrentSession()
    }
}