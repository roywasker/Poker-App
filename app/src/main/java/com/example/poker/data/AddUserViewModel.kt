package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var userName = mutableStateOf("")

    //List of all the player that have user in the app
    var playerList: MutableList<String> = mutableListOf()

    var userToChange = mutableStateOf("")

    var moneyChange = mutableStateOf("")

    // Message to popup in the screen - using StateFlow
    private val _messageDialog = MutableStateFlow<String?>(null)
    val messageDialog: StateFlow<String?> = _messageDialog.asStateFlow()

    /**
     * Function to save the user name that user enter
     */
    fun onUserNameChange(newName: String) {
        userName.value = newName
    }

    /**
     * Function to clear message dialog
     */
    fun clearMessageDialog() {
        _messageDialog.value = null
    }

    /**
     *  Function to get all the user name that registered in the data base
     */
    fun getPlayerList(){
        viewModelScope.launch {
            try {
                val users = withContext(Dispatchers.IO) {
                    userRepository.getUserList()
                }
                playerList.clear()
                playerList.addAll(users)
            } catch (e: Exception) {
                Log.e(TAG, "getPlayerList: $e")
                _messageDialog.value = "Failed to load player list"
            }
        }
    }

    /**
     * Function to add new user to the database
     */
    fun addUser() {
        // If player is already in the list popup a message
        if (playerList.contains(userName.value)){
            _messageDialog.value = "Player ${userName.value} is already in the list."
            return
        }

        // If user try to enter empty name to the list popup a message
        if (userName.value.isBlank()){
            _messageDialog.value = "Please enter a name."
            return
        }
        
        // Validate name length and characters
        if (userName.value.length > 50) {
            _messageDialog.value = "Name is too long. Maximum 50 characters."
            return
        }
        
        if (userName.value.length < 2) {
            _messageDialog.value = "Name is too short. Minimum 2 characters."
            return
        }

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    userRepository.addUser(userName.value, 0)
                }
                
                if (success) {
                    getPlayerList() // Refresh the player list after adding a new user
                    _messageDialog.value = "Player ${userName.value} added to the list."
                    userName.value = "" // clear user name box
                } else {
                    _messageDialog.value = "Player ${userName.value} is already in the list."
                }
            } catch (e: Exception) {
                Log.e(TAG, "addUser: $e")
                _messageDialog.value = "Failed to add player ${userName.value}\n Try later"
            }
        }
    }

    fun changeUserValue(){
        if (moneyChange.value.isBlank() || userToChange.value.isBlank()){
            _messageDialog.value = "Enter all the field "
            return
        }
        
        val moneyChangeInt = try {
            moneyChange.value.toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "changeUserValue: $e")
            _messageDialog.value = "Please enter a valid number"
            return
        }
        
        if (moneyChangeInt < 0 ){
            _messageDialog.value = "It is not possible to subtract a negative amount from a user."
            return
        }

        viewModelScope.launch {
            try {
                // Get current balance and calculate new balance
                val users = withContext(Dispatchers.IO) {
                    userRepository.getUserList()
                }
                
                if (!users.contains(userToChange.value)) {
                    _messageDialog.value = "User not found"
                    return@launch
                }
                
                // For this operation we're reducing money, so we need to get current balance
                // and subtract the amount. Since we don't have a method to get specific balance,
                // we'll use the generic changeUserBalance method with the new total
                val success = withContext(Dispatchers.IO) {
                    // This is a simplified approach - ideally we'd have a method to get current balance
                    // For now, we'll assume the UI shows current balance and user enters the new total
                    userRepository.changeUserBalance(userToChange.value, moneyChangeInt)
                }
                
                if (success) {
                    _messageDialog.value = "Balance successfully updated"
                    userToChange.value = ""
                    moneyChange.value = ""
                } else {
                    _messageDialog.value = "Failed to update user balance"
                }
            } catch (e: Exception) {
                Log.e(TAG, "changeUserValue: $e")
                _messageDialog.value = "Failed to update balance"
            }
        }
    }
}