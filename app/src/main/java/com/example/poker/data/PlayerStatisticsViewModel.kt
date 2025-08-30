package com.example.poker.data

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.poker.data.base.BaseViewModel
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
) : BaseViewModel() {

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


    init {
        loadPlayersList()
        loadAllPlayersStatistics()
    }

    private fun loadPlayersList() {
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { loadPlayersList() }
                    )
                } else {
                    Log.e(TAG, "loadPlayersList: $e")
                    showMessage("Error loading players list")
                }
                _playersList.value = emptyList()
            }
        ) {
            val players = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeout(3000L) {
                    gameRepository.getPlayersForStartGame()
                }
            }
            _playersList.value = players
        }
    }

    fun loadAllPlayersStatistics() {
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { loadAllPlayersStatistics() }
                    )
                } else {
                    Log.e(TAG, "loadAllPlayersStatistics: $e")
                    showMessage("Error loading statistics")
                }
                _allPlayersStatistics.value = emptyList()
            }
        ) {
            val allStats = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeout(3000L) {
                    gameRepository.getAllPlayersStatistics()
                }
            }
            _allPlayersStatistics.value = allStats
        }
    }

    fun selectPlayer(playerName: String) {
        selectedPlayer.value = playerName
        loadPlayerStatistics(playerName)
        loadPlayerHistoricalData(playerName)
    }

    private fun loadPlayerStatistics(playerName: String) {
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { loadPlayerStatistics(playerName) }
                    )
                } else {
                    Log.e(TAG, "loadPlayerStatistics: $e")
                    showMessage("Error loading player statistics")
                }
                _playerStatistics.value = null
            }
        ) {
            val stats = withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeout(3000L) {
                    gameRepository.getPlayerStatistics(playerName)
                }
            }
            _playerStatistics.value = stats
        }
    }

    private fun loadPlayerHistoricalData(playerName: String) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val historicalData = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(3000L) {
                        gameRepository.getPlayerHistoricalData(playerName)
                    }
                }
                _playerHistoricalData.value = historicalData
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "loadPlayerHistoricalData timeout: $e")
                _playerHistoricalData.value = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "loadPlayerHistoricalData: $e")
                _playerHistoricalData.value = emptyList()
            }
        }
    }

    // clearMessageDialog is inherited from BaseViewModel

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