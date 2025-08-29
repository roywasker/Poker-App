package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerBalanceViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {
    // Define the status of loading image to false - using StateFlow
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Player list - using StateFlow for better performance
    private val _playerList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val playerList: StateFlow<List<Pair<String, Int>>> = _playerList.asStateFlow()

    /**
     * Function to get all player balance
     */
    fun getPlayerBalance(){
        viewModelScope.launch {
            _loading.value = true
            
            try {
                val players = withContext(Dispatchers.IO) {
                    playerRepository.getAllPlayers()
                }
                _playerList.value = players
                _loading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "getPlayerBalance: $e")
                _loading.value = false
            }
        }
    }
}