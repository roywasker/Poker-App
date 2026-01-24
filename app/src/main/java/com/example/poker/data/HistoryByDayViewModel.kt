package com.example.poker.data

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.poker.data.base.BaseViewModel
import com.example.poker.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HistoryByDayViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : BaseViewModel() {

    //List of all the player that have user in the app
    var dateList:  MutableList<Pair<String, String>> = mutableListOf()

    // Player list - using StateFlow for better performance
    private val _playerList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val playerList: StateFlow<List<Pair<String, Int>>> = _playerList.asStateFlow()

    //data that your pick
    var dateSelected = mutableStateOf<Pair<String, String>>(Pair("Pick a date", ""))
    

    /**
     *  Function to get all date of game
     */
    fun getDateList(){
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { getDateList() }
                    )
                } else {
                    Log.e(TAG, "getDateList: $e")
                    showMessage("Failed to load dates. Please try again.")
                }
                dateList.clear()
            }
        ) {
            dateList.clear()
            val dates = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeout(3000L) {
                    gameRepository.getAllDates()
                }
            }
            dateList.addAll(dates)
        }
    }

    /**
     * Function to get all player balance by this date
     */
    fun getPlayerBalanceByDate(){
        if (dateSelected.value.first == "Pick a date"){
            showMessage("Please pick a date")
            return
        }
        
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { getPlayerBalanceByDate() }
                    )
                } else {
                    Log.e(TAG, "getPlayerBalanceByDate: $e")
                    showMessage("Failed to load player balances. Please try again.")
                }
                _playerList.value = emptyList()
            }
        ) {
            val players = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeout(3000L) {
                    gameRepository.getPlayerBalanceByDate(dateSelected.value.second)
                }
            }
            _playerList.value = players
        }
    }

}