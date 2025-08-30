package com.example.poker.data

import com.example.poker.data.base.BaseViewModel
import com.example.poker.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for displaying player balances
 * Refactored to extend BaseViewModel and use proper state management
 */
@HiltViewModel
class PlayerBalanceViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : BaseViewModel() {
    
    // Player list - using StateFlow for better performance
    private val _playerList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val playerList: StateFlow<List<Pair<String, Int>>> = _playerList.asStateFlow()
    
    // Search/filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Filtered player list based on search
    private val _filteredPlayerList = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val filteredPlayerList: StateFlow<List<Pair<String, Int>>> = _filteredPlayerList.asStateFlow()
    
    /**
     * Get all player balances
     * Fixed typo from gerPlayerBalance to getPlayerBalance
     */
    fun getPlayerBalance() {
        launchWithLoading(
            onError = { e ->
                if (e is kotlinx.coroutines.TimeoutCancellationException || 
                    e.message?.contains("timeout", ignoreCase = true) == true) {
                    showNetworkError(
                        "Connection timeout. Please check your internet connection.",
                        retryAction = { getPlayerBalance() }
                    )
                } else {
                    showMessage("Failed to load player balances: ${e.message}")
                }
                _playerList.value = emptyList()
            }
        ) {
            val players = kotlinx.coroutines.withTimeout(3000L) {
                playerRepository.getAllPlayers()
            }
            _playerList.value = players
            applyFilter()
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }
    
    /**
     * Apply search filter to player list
     */
    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
        val players = _playerList.value
        
        _filteredPlayerList.value = if (query.isBlank()) {
            players
        } else {
            players.filter { (name, _) ->
                name.lowercase().contains(query)
            }
        }
    }
    
    /**
     * Refresh player list
     */
    fun refresh() {
        getPlayerBalance()
    }
    
    /**
     * Get total balance across all players
     */
    fun getTotalBalance(): Int {
        return _playerList.value.sumOf { it.second }
    }
    
    /**
     * Get player count
     */
    fun getPlayerCount(): Int {
        return _playerList.value.size
    }
    
    /**
     * Get average balance
     */
    fun getAverageBalance(): Int {
        val count = getPlayerCount()
        return if (count > 0) {
            getTotalBalance() / count
        } else {
            0
        }
    }
    
    /**
     * Get top players by balance
     */
    fun getTopPlayers(limit: Int = 5): List<Pair<String, Int>> {
        return _playerList.value
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Get players with negative balance
     */
    fun getPlayersInDebt(): List<Pair<String, Int>> {
        return _playerList.value.filter { it.second < 0 }
    }
    
    /**
     * Export player data as CSV string
     */
    fun exportAsCSV(): String {
        val header = "Player Name,Balance\n"
        val data = _playerList.value.joinToString("\n") { (name, balance) ->
            "$name,$balance"
        }
        return header + data
    }
}