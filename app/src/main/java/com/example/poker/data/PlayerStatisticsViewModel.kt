package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.data.repository.GameRepository
import com.example.poker.data.repository.PlayerStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerStatisticsViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    // Loading state
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // All players list for dropdown
    private val _playersList = MutableStateFlow<List<String>>(emptyList())
    val playersList: StateFlow<List<String>> = _playersList.asStateFlow()

    // Selected player
    var selectedPlayer = mutableStateOf<String?>(null)

    // Individual player statistics
    private val _playerStatistics = MutableStateFlow<PlayerStatistics?>(null)
    val playerStatistics: StateFlow<PlayerStatistics?> = _playerStatistics.asStateFlow()

    // All players statistics for comparison
    private val _allPlayersStatistics = MutableStateFlow<List<PlayerStatistics>>(emptyList())
    val allPlayersStatistics: StateFlow<List<PlayerStatistics>> = _allPlayersStatistics.asStateFlow()

    // Historical data for charts
    private val _playerHistoricalData = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val playerHistoricalData: StateFlow<List<Pair<String, Int>>> = _playerHistoricalData.asStateFlow()

    // Error message dialog
    private val _messageDialog = MutableStateFlow<String?>(null)
    val messageDialog: StateFlow<String?> = _messageDialog.asStateFlow()

    init {
        loadPlayersList()
        loadAllPlayersStatistics()
    }

    private fun loadPlayersList() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val players = withContext(Dispatchers.IO) {
                    gameRepository.getPlayersForStartGame()
                }
                _playersList.value = players
            } catch (e: Exception) {
                Log.e(TAG, "loadPlayersList: $e")
                _messageDialog.value = "Error loading players list"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadAllPlayersStatistics() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val allStats = withContext(Dispatchers.IO) {
                    gameRepository.getAllPlayersStatistics()
                }
                _allPlayersStatistics.value = allStats
            } catch (e: Exception) {
                Log.e(TAG, "loadAllPlayersStatistics: $e")
                _messageDialog.value = "Error loading statistics"
            } finally {
                _loading.value = false
            }
        }
    }

    fun selectPlayer(playerName: String) {
        selectedPlayer.value = playerName
        loadPlayerStatistics(playerName)
        loadPlayerHistoricalData(playerName)
    }

    private fun loadPlayerStatistics(playerName: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val stats = withContext(Dispatchers.IO) {
                    gameRepository.getPlayerStatistics(playerName)
                }
                _playerStatistics.value = stats
            } catch (e: Exception) {
                Log.e(TAG, "loadPlayerStatistics: $e")
                _messageDialog.value = "Error loading player statistics"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun loadPlayerHistoricalData(playerName: String) {
        viewModelScope.launch {
            try {
                val historicalData = withContext(Dispatchers.IO) {
                    gameRepository.getPlayerHistoricalData(playerName)
                }
                _playerHistoricalData.value = historicalData
            } catch (e: Exception) {
                Log.e(TAG, "loadPlayerHistoricalData: $e")
            }
        }
    }

    fun clearMessageDialog() {
        _messageDialog.value = null
    }

    fun getTopPerformers(): List<PlayerStatistics> {
        return _allPlayersStatistics.value
            .sortedByDescending { it.currentBalance }
            .take(3)
    }

    fun getMostActivePlayer(): PlayerStatistics? {
        return _allPlayersStatistics.value
            .maxByOrNull { it.totalGames }
    }

    fun getBestSingleGamePerformance(): Pair<String, Int>? {
        return _allPlayersStatistics.value
            .maxByOrNull { it.bestGame }
            ?.let { it.playerName to it.bestGame }
    }

    fun getWorstSingleGamePerformance(): Pair<String, Int>? {
        return _allPlayersStatistics.value
            .minByOrNull { it.worstGame }
            ?.let { it.playerName to it.worstGame }
    }

    fun getWinRatePercentage(stats: PlayerStatistics): Double {
        return if (stats.totalGames > 0) {
            (stats.gamesWon.toDouble() / stats.totalGames.toDouble()) * 100
        } else {
            0.0
        }
    }

    fun getCurrentStreakDescription(streak: Int): String {
        return when {
            streak > 0 -> "$streak game winning streak"
            streak < 0 -> "${kotlin.math.abs(streak)} game losing streak"
            else -> "No current streak"
        }
    }
}