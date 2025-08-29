package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.poker.data.database.PlayerSessionRepository
import com.example.poker.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class StartGameViewModel @Inject constructor(
    private val sessionRepository: PlayerSessionRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    // Set the default of row to be 4
    var numOfRows = mutableIntStateOf(4)

    // Define array of 9 player name
    var nameOfPlayerArray = Array(9) { mutableStateOf("") }

    // Define array of 9 how much chip player buy
    var buyMoneyArray = Array(9) { mutableStateOf("")}

    // Define array of 9 how much chip player return in the end of the game
    var returnMoneyArray = Array(9) { mutableStateOf("") }

    //List of all the player that have user in the app - Thread-safe
    var playerList = MutableStateFlow<List<String>>(emptyList())

    //List of all the player that chosen in this current game - Thread-safe
    private var playerListChosen = MutableStateFlow<List<String>>(emptyList())

    // Message to popup in the screen - using StateFlow
    private val _messageDialog = MutableStateFlow<String?>(null)
    val messageDialog: StateFlow<String?> = _messageDialog.asStateFlow()

    // Define the status of loading image to false - using StateFlow
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Track if there's an active session - using StateFlow
    private val _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession.asStateFlow()

    init {
        // Check for existing session on startup
        viewModelScope.launch {
            _hasActiveSession.value = sessionRepository.hasActiveSession()
            if (_hasActiveSession.value) {
                loadSavedSession()
            }
        }
    }

    /**
     * Function to add new player to the list of chosen player
     */
    fun addPlayerToList(name : String){

        // Remove from the list of all available player - Thread-safe update
        playerList.value = playerList.value.filter { it != name }
        playerListChosen.value = playerListChosen.value + name
        
        // Save session after any change
        saveCurrentSession()
    }

    /**
     * Function to remove player from chosen list
     */
    fun removePlayerFromList(name : String){

        // Add to the list of all available player - Thread-safe update
        playerList.value = playerList.value + name
        playerListChosen.value = playerListChosen.value.filter { it != name }
        
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
            try {
                val buyAmount = buyMoneyArray[index].value.toIntOrNull() ?: 0
                val returnAmount = returnMoneyArray[index].value.toIntOrNull() ?: 0
                val currPlayerBalance = returnAmount - buyAmount
                balanceAfterGame[index] = currPlayerBalance
                sumMoney += currPlayerBalance
            } catch (e: Exception) {
                _messageDialog.value = "Invalid amount entered for ${nameOfPlayerArray[index].value}"
                return false
            }
        }

        // If sum money is negative we have extra money
        if (sumMoney<0){
            _messageDialog.value = "You have a extra of ${abs(sumMoney)} shekels."
            return false

        // If sum money is positive we have deficit money
        }else if (sumMoney>0){
            _messageDialog.value = "You have a deficit of $sumMoney shekels."
            return false

        // If sum money is zero we don't have extra or deficit
        }else {
            calcTransferMoney(balanceAfterGame)
            updateBalanceInDataBase(balanceAfterGame, nameOfPlayerArray)
        }
        return true
    }

    /**
     * Function to reset the data box in game screen
     */
    private fun resetPageData(){
        buyMoneyArray = Array(9) { mutableStateOf("")}
        returnMoneyArray = Array(9) { mutableStateOf("")}
        playerListChosen.value = emptyList()
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
            _hasActiveSession.value = true
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
            
            // Rebuild chosen players list - Thread-safe
            val chosenPlayers = mutableListOf<String>()
            for (i in 0 until data.numOfRows) {
                val playerName = data.playerNames[i].value
                if (playerName.isNotEmpty()) {
                    chosenPlayers.add(playerName)
                }
            }
            playerListChosen.value = chosenPlayers
        }
    }

    /**
     * Function to clear saved session
     */
    fun clearSavedSession() {
        viewModelScope.launch {
            sessionRepository.clearSession()
            _hasActiveSession.value = false
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
     * Function to clear message dialog
     */
    fun clearMessageDialog() {
        _messageDialog.value = null
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
                val playerNames = nameOfPlayer.map { it.value }.toTypedArray()
                val success = withContext(Dispatchers.IO) {
                    gameRepository.updatePlayerBalance(balanceAfterGame, playerNames)
                }
                
                if (success) {
                    //reset all the array form data
                    resetPageData()
                    
                    // Clear saved session after successful game completion
                    sessionRepository.clearSession()
                    _hasActiveSession.value = false
                } else {
                    _messageDialog.value = "Failed to update game data. Please try again."
                }
            } catch (e: Exception) {
                _messageDialog.value = "Failed to update game data. Please try again."
                Log.e(TAG, "updateBalanceInDataBase: $e")
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
                _messageDialog.value = "Please fill all the field."
                return false
            }
            val isValid1 = buyMoneyArray[index].value.all { it.isDigit() }
            val isValid2 = returnMoneyArray[index].value.all { it.isDigit() }
            if (!isValid1||!isValid2) {
                _messageDialog.value = "Please enter numbers only."
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
            _loading.value = true
            
            try {
                val players = withContext(Dispatchers.IO) {
                    gameRepository.getPlayersForStartGame()
                }
                
                // Thread-safe update
                val currentChosen = playerListChosen.value
                playerList.value = players.filter { playerName -> 
                    !currentChosen.contains(playerName)
                }
                _loading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "getPlayerListToStartGame: $e")
                _loading.value = false
            }
        }
    }

    /**
     * Function to remove row form the screen
     */
    fun removeRow(index : Int){
        val playerName = nameOfPlayerArray[index].value

        // If the player that chosen in this row not contain in player list add him - Thread-safe
        if (playerName.isNotEmpty() && !playerList.value.contains(playerName)) {
            playerList.value = playerList.value + playerName
        }

        // reset the variable to default value
        nameOfPlayerArray[index]= mutableStateOf("")
        buyMoneyArray[index] =  mutableStateOf("")
        returnMoneyArray[index] =  mutableStateOf("")
        
        // Save session after row removal
        saveCurrentSession()
    }
}