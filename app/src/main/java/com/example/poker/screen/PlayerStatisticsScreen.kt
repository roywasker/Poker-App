package com.example.poker.screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var isComparisonMode by remember { mutableStateOf(false) }
    var selectedPlayersForComparison by remember { mutableStateOf(emptyList<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.LightGray
                ),
                title = {
                    Text(
                        text = selectedPlayer?.let { "$it Statistics" } ?: "Player Statistics",
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
            // Player Selection Dropdown or Comparison Mode
            if (!isComparisonMode) {
                PlayerSelectionDropdown(
                    playersList = playersList,
                    selectedPlayer = selectedPlayer,
                    onPlayerSelected = { viewModel.selectPlayer(it) }
                )
                
                // Comparison Mode Button - Centered
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { 
                            isComparisonMode = true
                            selectedPlayersForComparison = emptyList()
                        },
                        modifier = Modifier
                            .height(60.dp)
                            .width(250.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.DarkGray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Compare Players",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "Compare Players",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                ComparisonModeSelector(
                    playersList = playersList,
                    selectedPlayers = selectedPlayersForComparison,
                    onPlayersSelected = { selectedPlayersForComparison = it },
                    onExitComparison = { 
                        isComparisonMode = false
                        selectedPlayersForComparison = emptyList()
                    }
                )
            }

            // Content based on mode
            if (isComparisonMode && selectedPlayersForComparison.size >= 2) {
                // Comparison Statistics Display
                PlayerComparisonDisplay(
                    selectedPlayers = selectedPlayersForComparison,
                    allPlayersStatistics = allPlayersStatistics,
                    viewModel = viewModel
                )
            } else if (!isComparisonMode) {
                // Global Statistics Section (when no specific player selected)
                if (selectedPlayer.isNullOrEmpty()) {
                    GlobalStatisticsSection(
                        allStats = allPlayersStatistics,
                        viewModel = viewModel
                    )
                }

                // Individual Player Statistics
                selectedPlayer?.let { player ->
                    playerStatistics?.let { stats ->
                        PlayerDetailedStatistics(stats, viewModel)
                    }
                }
            } else if (isComparisonMode && selectedPlayersForComparison.size < 2) {
                // Show instruction when in comparison mode but not enough players selected
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select at least 2 players to compare",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Top Performers",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "Top Performers",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                viewModel.getTopPerformers().forEachIndexed { index, player ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (index) {
                                0 -> Color(0xFFFFD700).copy(alpha = 0.1f) // Gold
                                1 -> Color(0xFFC0C0C0).copy(alpha = 0.1f) // Silver
                                2 -> Color(0xFFCD7F32).copy(alpha = 0.1f) // Bronze
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Star
                                        1 -> Icons.Default.Star
                                        2 -> Icons.Default.Star
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = "Rank ${index + 1}",
                                    tint = when (index) {
                                        0 -> Color(0xFFFFD700)
                                        1 -> Color(0xFFC0C0C0)
                                        2 -> Color(0xFFCD7F32)
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = "${index + 1}. ${player.playerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index < 3) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (player.currentBalance >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (player.currentBalance >= 0) "Profit" else "Loss",
                                    tint = if (player.currentBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                                Text(
                                    text = "${player.currentBalance} ₪",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (player.currentBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Most active player
                viewModel.getMostActivePlayer()?.let { mostActive ->
                    EnhancedStatisticRow(
                        icon = Icons.Default.AccountBox,
                        label = "Most Active Player",
                        value = "${mostActive.playerName} (${mostActive.totalGames} games)",
                        iconTint = Color(0xFF2196F3)
                    )
                }

                // Best single game
                viewModel.getBestSingleGamePerformance()?.let { (player, amount) ->
                    EnhancedStatisticRow(
                        icon = Icons.Default.KeyboardArrowUp,
                        label = "Best Single Game",
                        value = "$player (+$amount ₪)",
                        iconTint = Color(0xFF4CAF50)
                    )
                }

                // Worst single game
                viewModel.getWorstSingleGamePerformance()?.let { (player, amount) ->
                    EnhancedStatisticRow(
                        icon = Icons.Default.KeyboardArrowDown,
                        label = "Worst Single Game",
                        value = "$player ($amount ₪)",
                        iconTint = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonModeSelector(
    playersList: List<String>,
    selectedPlayers: List<String>,
    onPlayersSelected: (List<String>) -> Unit,
    onExitComparison: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compare Players (Select 2-5)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onExitComparison,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text(
                        text = "Exit",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selected players chips
            if (selectedPlayers.isNotEmpty()) {
                Text(
                    text = "Selected Players:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedPlayers.forEach { player ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { 
                                        onPlayersSelected(selectedPlayers - player)
                                    },
                                    modifier = Modifier.size(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text("×", color = Color.DarkGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Player selection dropdown
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded && selectedPlayers.size < 5 },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "Add Player to Compare",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    enabled = selectedPlayers.size < 5,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    playersList.filter { it !in selectedPlayers }.forEach { player ->
                        DropdownMenuItem(
                            text = { Text(player) },
                            onClick = {
                                if (selectedPlayers.size < 5) {
                                    onPlayersSelected(selectedPlayers + player)
                                }
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
fun PlayerComparisonDisplay(
    selectedPlayers: List<String>,
    allPlayersStatistics: List<PlayerStatistics>,
    viewModel: PlayerStatisticsViewModel
) {
    val playersToCompare = allPlayersStatistics.filter { it.playerName in selectedPlayers }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Comparison Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Compare Players",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "Player Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Comparing ${selectedPlayers.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // Comparison Grid
        playersToCompare.forEach { playerStats ->
            PlayerComparisonCard(
                stats = playerStats,
                viewModel = viewModel,
                isTopPerformer = playersToCompare.maxByOrNull { it.currentBalance } == playerStats
            )
        }

        // Quick Comparison Summary
        if (playersToCompare.size >= 2) {
            ComparisonSummaryCard(playersToCompare)
        }
    }
}

@Composable
fun PlayerComparisonCard(
    stats: PlayerStatistics,
    viewModel: PlayerStatisticsViewModel,
    isTopPerformer: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopPerformer) Color(0xFFFFD700).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopPerformer) 6.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Player Name Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTopPerformer) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Top Performer",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = stats.playerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Balance with indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (stats.currentBalance >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (stats.currentBalance >= 0) "Profit" else "Loss",
                        tint = if (stats.currentBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = "${stats.currentBalance} ₪",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.currentBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Metrics in Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComparisonMetric(
                    label = "Games",
                    value = stats.totalGames.toString(),
                    modifier = Modifier.weight(1f)
                )
                ComparisonMetric(
                    label = "Win Rate",
                    value = "${String.format("%.1f", viewModel.getWinRatePercentage(stats))}%",
                    modifier = Modifier.weight(1f)
                )
                ComparisonMetric(
                    label = "Avg/Game",
                    value = "${String.format("%.1f", stats.averageWinLoss)} ₪",
                    modifier = Modifier.weight(1f)
                )
                ComparisonMetric(
                    label = "Best Game",
                    value = "+${stats.bestGame} ₪",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ComparisonMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComparisonSummaryCard(playersToCompare: List<PlayerStatistics>) {
    val bestPerformer = playersToCompare.maxByOrNull { it.currentBalance }
    val mostActive = playersToCompare.maxByOrNull { it.totalGames }
    val bestWinRate = playersToCompare.maxByOrNull { if (it.totalGames > 0) it.gamesWon.toDouble() / it.totalGames else 0.0 }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Comparison Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            bestPerformer?.let {
                EnhancedStatisticRow(
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "Best Overall Balance",
                    value = "${it.playerName} (${it.currentBalance} ₪)",
                    iconTint = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            mostActive?.let {
                EnhancedStatisticRow(
                    icon = Icons.Default.AccountBox,
                    label = "Most Active",
                    value = "${it.playerName} (${it.totalGames} games)",
                    iconTint = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            bestWinRate?.let {
                val winRate = if (it.totalGames > 0) (it.gamesWon.toDouble() / it.totalGames) * 100 else 0.0
                EnhancedStatisticRow(
                    icon = Icons.Default.Star,
                    label = "Best Win Rate",
                    value = "${it.playerName} (${String.format("%.1f", winRate)}%)",
                    iconTint = Color(0xFFFFD700)
                )
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
fun EnhancedStatisticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: Color = Color.Gray
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
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