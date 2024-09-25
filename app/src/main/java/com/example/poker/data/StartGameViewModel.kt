package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class StartGameViewModel: ViewModel() {

    var numOfRows = mutableIntStateOf(4)
    var nameOfPlayerArray = Array(9) { mutableStateOf("") }
    var buyMoneyArray = Array(9) { mutableStateOf("")}
    var returnMoneyArray = Array(9) { mutableStateOf("") }
    var playerList: MutableList<String> = mutableListOf()
    private val databaseRef = FirebaseDatabase.getInstance().getReference("PlayersList")
    private var playerListChosen: MutableList<String> = mutableListOf()
    var massageDialog =  mutableStateOf<String?>(null)
    var loading = mutableStateOf(false)
    val transferLog = mutableListOf<String>()

    fun addPlayerToList(name : String){
        playerList.remove(name)
        playerListChosen.add(name)
    }
    fun removePlayerFromList(name : String){
        playerList.add(name)
        playerListChosen.remove(name)
    }
    fun finishGameButton() : Int {
        if (!CheackInput()){
            return 0
        }
        var sumMoney = 0
        var balanceAfterGame = Array(numOfRows.intValue){0}
        for (index in 0..<numOfRows.intValue){
            val currPlayerBalance = returnMoneyArray[index].value.toInt() - buyMoneyArray[index].value.toInt()
            balanceAfterGame[index] = currPlayerBalance
            sumMoney+=currPlayerBalance
        }
        if (sumMoney<0){
            massageDialog.value = "You have a extra of $sumMoney shekels."
            return 0
        }else if (sumMoney>0){
            massageDialog.value = "You have a deficit of $sumMoney shekels."
            return 0
        }else {
            updateBalanceInDataBase(balanceAfterGame)
            calcTransferMoney(balanceAfterGame)
        }
        return 1
    }

    private fun updateBalanceInDataBase(balanceAfterGame: Array<Int>) {
        databaseRef.get().addOnSuccessListener { snapshot ->
            for (playerSnapshot in snapshot.children) {
                val playerName = playerSnapshot.child("name").getValue(String::class.java)
                var playerBalance = playerSnapshot.child("balance").getValue(Int::class.java)
                if (playerListChosen.contains(playerName) && playerBalance != null &&playerName != null) {
                    val playerIndex = nameOfPlayerArray.indexOfFirst { it.value == playerName }
                    if (playerIndex != -1) {
                        playerBalance += balanceAfterGame[playerIndex]
                        playerSnapshot.ref.child("balance").setValue(playerBalance)
                    }
                }
            }
        }
    }

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
        while (gainers.isNotEmpty() && losers.isNotEmpty()){
            val gainPlayer = gainers[0]
            val loserPlayer = losers[0]
            val transferAmount = minOf(gainPlayer.second,loserPlayer.second)
            if (transferAmount==gainPlayer.second){
                gainers.removeAt(0)
            }else if (transferAmount<gainPlayer.second){
                gainers[0] = gainPlayer.copy(second = gainPlayer.second - transferAmount)
                gainers.sortByDescending { it.second }
            }
            if (transferAmount==loserPlayer.second){
                losers.removeAt(0)
            }else if (transferAmount<loserPlayer.second){
                losers[0] = loserPlayer.copy(second = loserPlayer.second - transferAmount)
                losers.sortByDescending { it.second }
            }
            transferLog.add("${loserPlayer.first} transfer $transferAmount to ${gainPlayer.first}")
            Log.d("StartGameViewModel","${loserPlayer.first} transfer $transferAmount to ${gainPlayer.first}")
        }
    }

    private fun CheackInput(): Boolean {
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

    fun removeRow(index : Int){
        val playerName = nameOfPlayerArray[index].value
        if (!playerList.contains(playerName)) {
            playerList.add(playerName)
        }
        nameOfPlayerArray[index]= mutableStateOf("")
        buyMoneyArray[index] =  mutableStateOf("0")
        returnMoneyArray[index] =  mutableStateOf("0")
    }
}