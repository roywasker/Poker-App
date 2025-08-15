package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HistoryByDayViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    //List of all the player that have user in the app
    var dateList:  MutableList<Pair<String, String>> = mutableListOf()

    // Player list - using StateFlow for better performance
    private val _playerList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val playerList: StateFlow<List<Pair<String, Int>>> = _playerList.asStateFlow()

    // Define the status of loading image to false - using StateFlow
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    //data that your pick
    var dateSelected = mutableStateOf<Pair<String, String>>(Pair("Pick a date", ""))

    // Message to popup in the screen - using StateFlow
    private val _messageDialog = MutableStateFlow<String?>(null)
    val messageDialog: StateFlow<String?> = _messageDialog.asStateFlow()

    /**
     *  Function to get all date of game
     */
    fun getDateList(){
        viewModelScope.launch {
            _loading.value = true
            dateList.clear()
            
            try {
                val dates = withContext(Dispatchers.IO) {
                    gameRepository.getAllDates()
                }
                dateList.addAll(dates)
                _loading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "getDateList: $e")
                _loading.value = false
            }
        }
    }

    /**
     * Function to get all player balance by this date
     */
    fun getPlayerBalanceByDate(){
        if (dateSelected.value.first == "Pick a date"){
            _messageDialog.value = "Please pick a date"
            return
        }
        
        viewModelScope.launch {
            _loading.value = true
            
            try {
                val players = withContext(Dispatchers.IO) {
                    gameRepository.getPlayerBalanceByDate(dateSelected.value.second)
                }
                _playerList.value = players
                _loading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "getPlayerBalanceByDate: $e")
                _loading.value = false
            }
        }
    }

    /**
     * Function to clear message dialog
     */
    fun clearMessageDialog() {
        _messageDialog.value = null
    }
}