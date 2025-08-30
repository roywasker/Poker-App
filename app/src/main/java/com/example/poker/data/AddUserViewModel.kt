package com.example.poker.data

import android.util.Log
import com.example.poker.data.base.BaseViewModel
import com.example.poker.data.repository.UserRepository
import com.example.poker.util.ValidationResult
import com.example.poker.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for adding and managing users
 * Refactored to use StateFlow and proper validation
 */
@HiltViewModel
class AddUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel() {

    // User input states - using StateFlow instead of mutableStateOf
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()
    
    private val _userToChange = MutableStateFlow("")
    val userToChange: StateFlow<String> = _userToChange.asStateFlow()
    
    private val _moneyChange = MutableStateFlow("")
    val moneyChange: StateFlow<String> = _moneyChange.asStateFlow()
    
    // Player list - using StateFlow for better performance
    private val _playerList = MutableStateFlow<List<String>>(emptyList())
    val playerList: StateFlow<List<String>> = _playerList.asStateFlow()
    
    init {
        loadPlayerList()
    }
    
    /**
     * Update user name input
     */
    fun onUserNameChange(newName: String) {
        // Sanitize input to prevent injection
        _userName.value = ValidationUtils.sanitizeInput(newName)
    }
    
    /**
     * Update user to change selection
     */
    fun onUserToChangeSelected(user: String) {
        _userToChange.value = user
    }
    
    /**
     * Update money change amount
     */
    fun onMoneyChangeUpdate(amount: String) {
        // Only allow numeric input
        _moneyChange.value = amount.filter { it.isDigit() || it == '-' }
    }

    
    /**
     * Load player list from repository with timeout handling
     */
    private fun loadPlayerList() {
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { loadPlayerList() }
                    )
                } else {
                    showMessage("Failed to load players: ${e.message}")
                }
            }
        ) {
            val players = userRepository.getUserList()
            _playerList.value = players.sorted() // Sort alphabetically
        }
    }
    
    /**
     * Add a new user with validation
     */
    fun addUser() {
        val name = _userName.value.trim()

        // Validate username
        when (val validation = ValidationUtils.validateUsername(name)) {
            is ValidationResult.Error -> {
                showMessage(validation.message)
                return
            }
            is ValidationResult.Success -> {
                // Continue with adding user
            }
        }
        
        // Check if user already exists locally
        if (_playerList.value.any { it.equals(name, ignoreCase = true) }) {
            showMessage("User '$name' already exists")
            return
        }
        
        // Add user
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout while adding user. Please check your internet connection.",
                        retryAction = { addUser() }
                    )
                } else {
                    showMessage("Failed to add user: ${e.message}")
                }
            }
        ) {
            val success = userRepository.addUser(name, 0)
            if (success) {
                showMessage("User '$name' added successfully")
                clearInputs()
                loadPlayerList() // Reload the list
            } else {
                showMessage("Failed to add user. User may already exist.")
            }
        }
    }
    
    /**
     * Clear all input fields
     */
    private fun clearInputs() {
        _userName.value = ""
        clearBalanceInputs()
    }
    
    /**
     * Clear balance change inputs
     */
    private fun clearBalanceInputs() {
        _userToChange.value = ""
        _moneyChange.value = ""
    }
    
    /**
     * Refresh player list
     */
    fun refreshPlayerList() {
        loadPlayerList()
    }
    
    /**
     * Get the list of players for the game
     */
    fun getUserList() {
        loadPlayerList()
    }
}