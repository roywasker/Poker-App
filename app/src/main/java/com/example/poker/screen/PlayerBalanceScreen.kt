package com.example.poker.screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.poker.data.PlayerBalanceViewModel
import com.example.poker.route.Routes

@SuppressLint("ContextCastToActivity")
@Composable
fun PlayerBalanceScreen(
    navController: NavHostController,
    viewModel: PlayerBalanceViewModel = hiltViewModel()
){
    val loading by viewModel.loading.collectAsState()
    if (loading) {
        LoadingScreen()// Display a loading screen
    } else {
        PlayerBalanceComponent(navController,viewModel)
    }

    LaunchedEffect(Unit) {
        if (!loading) {
            viewModel.gerPlayerBalance()
        }
    }

    BackHandler {
        navController.navigate(Routes.homeScreen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBalanceComponent(navController: NavHostController, viewModel: PlayerBalanceViewModel) {
    val playerBalanceList by viewModel.playerList.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.LightGray),
                title = {
                    Text(text = "", color = Color.White)
                },
                // Set back arrow to go to the home screen
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.homeScreen) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .padding(start = 6.dp, end = 8.dp)
                                .size(40.dp)
                        )
                    }
                })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Table Header
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                BasicText(
                    text = "Name",
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 22.sp) // Set font size using TextStyle
                )
                BasicText(
                    text = "Number",
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontSize = 22.sp) // Set font size using TextStyle
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) // Spacing after header

            // Player Balance List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playerBalanceList) { (name, number) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        BasicText(text = name, modifier = Modifier.weight(1f), style = TextStyle(fontSize = 18.sp))
                        BasicText(text = number.toString(), modifier = Modifier.weight(1f), style = TextStyle(fontSize = 18.sp))
                    }
                    Spacer(modifier = Modifier.height(6.dp)) // Add some spacing between rows
                }
            }
        }
    }
}