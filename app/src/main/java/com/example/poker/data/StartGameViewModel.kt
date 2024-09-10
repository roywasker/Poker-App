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

    fun addPlayerToList(name : String){
        playerList.remove(name)
        playerListChosen.add(name)
    }
    fun removePlayerFromList(name : String){
        playerList.add(name)
        playerListChosen.remove(name)
    }
    fun finishGameButton(){
        if (!CheackInput()){
            return
        }
        var balanceAfterGame = Array(numOfRows.intValue){0}
        for (index in 0..<numOfRows.intValue){
            val currPlayerBalance = returnMoneyArray[index].value.toInt() - buyMoneyArray[index].value.toInt()
            balanceAfterGame[index] = currPlayerBalance
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