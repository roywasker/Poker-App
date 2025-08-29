package com.example.poker.screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.poker.data.PlayerStatisticsViewModel
import com.example.poker.data.repository.PlayerStatistics
import com.example.poker.route.Routes
import kotlin.math.absoluteValue

@SuppressLint("ContextCastToActivity")
@Composable
fun PlayerStatisticsScreen(
    navController: NavHostController,
    viewModel: PlayerStatisticsViewModel = hiltViewModel()
) {
    val loading by viewModel.loading.collectAsState()
    val messageDialog by viewModel.messageDialog.collectAsState()

    if (loading) {
        LoadingScreen()
    } else {
        PlayerStatisticsComponent(navController, viewModel)
    }

    // Show error dialog if needed
    messageDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessageDialog() },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { viewModel.clearMessageDialog() }) {
                    Text("OK")
                }
            }
        )
    }

    BackHandler {
        navController.navigate(Routes.homeScreen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatisticsComponent(
    navController: NavHostController,
    viewModel: PlayerStatisticsViewModel
) {
    val playersList by viewModel.playersList.collectAsState()
    val selectedPlayer by viewModel.selectedPlayer
    val playerStatistics by viewModel.playerStatistics.collectAsState()
    val allPlayersStatistics by viewModel.allPlayersStatistics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.LightGray
                ),
                title = {
                    Text(
                        text = "Player Statistics",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.homeScreen) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .padding(start = 6.dp, end = 8.dp)
                                .size(40.dp),
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player Selection Dropdown
            PlayerSelectionDropdown(
                playersList = playersList,
                selectedPlayer = selectedPlayer,
                onPlayerSelected = { viewModel.selectPlayer(it) }
            )

            // Global Statistics Section
            GlobalStatisticsSection(
                allStats = allPlayersStatistics,
                viewModel = viewModel
            )

            // Individual Player Statistics
            selectedPlayer?.let { player ->
                playerStatistics?.let { stats ->
                    PlayerDetailedStatistics(stats, viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionDropdown(
    playersList: List<String>,
    selectedPlayer: String?,
    onPlayerSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Player",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPlayer ?: "Choose a player",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    playersList.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player) },
                            onClick = {
                                onPlayerSelected(player)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalStatisticsSection(
    allStats: List<PlayerStatistics>,
    viewModel: PlayerStatisticsViewModel
) {
    if (allStats.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Global Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Top 3 performers
                Text(
                    text = "Top Performers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                viewModel.getTopPerformers().forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${player.playerName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${player.currentBalance} ₪",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (player.currentBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Most active player
                viewModel.getMostActivePlayer()?.let { mostActive ->
                    StatisticRow("Most Active Player", "${mostActive.playerName} (${mostActive.totalGames} games)")
                }

                // Best single game
                viewModel.getBestSingleGamePerformance()?.let { (player, amount) ->
                    StatisticRow("Best Single Game", "$player (+$amount ₪)")
                }

                // Worst single game
                viewModel.getWorstSingleGamePerformance()?.let { (player, amount) ->
                    StatisticRow("Worst Single Game", "$player ($amount ₪)")
                }
            }
        }
    }
}

@Composable
fun PlayerDetailedStatistics(
    stats: PlayerStatistics,
    viewModel: PlayerStatisticsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${stats.playerName} Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Current Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (stats.currentBalance >= 0) Color(0xFFE8F5E8) else Color(0xFFFBE9E9)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "${stats.currentBalance} ₪",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.currentBalance >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // Game Statistics
            StatisticRow("Total Games", stats.totalGames.toString())
            StatisticRow("Games Won", "${stats.gamesWon} (${String.format("%.1f", viewModel.getWinRatePercentage(stats))}%)")
            StatisticRow("Games Lost", "${stats.gamesLost} (${String.format("%.1f", 100 - viewModel.getWinRatePercentage(stats))}%)")

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Statistics
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StatisticRow("Total Winnings", "${stats.totalWinnings} ₪")
            StatisticRow("Average Per Game", "${String.format("%.1f", stats.averageWinLoss)} ₪")
            StatisticRow("Best Game", "+${stats.bestGame} ₪")
            StatisticRow("Worst Game", "${stats.worstGame} ₪")

            Spacer(modifier = Modifier.height(16.dp))

            // Streak Statistics
            Text(
                text = "Streaks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StatisticRow("Current Streak", viewModel.getCurrentStreakDescription(stats.currentStreak))
            StatisticRow("Longest Win Streak", "${stats.longestWinStreak} games")
            StatisticRow("Longest Loss Streak", "${stats.longestLossStreak} games")
        }
    }
}

@Composable
fun StatisticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}