package com.example.poker.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.poker.data.TransferLogViewModel
import com.example.poker.route.Routes

@Composable
fun TransferLogScreen(navController: NavHostController) {
    // ViewModel instance
    val transferLogViewModel: TransferLogViewModel = viewModel()

    BackHandler {
        navController.navigate(Routes.homeScreen)
    }

    // Pass ViewModel to the component
    TransferLogComponent(navController, transferLogViewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferLogComponent(navController: NavHostController, transferLogViewModel: TransferLogViewModel) {
    // Observe the transfer log from the ViewModel
    val transferLog = transferLogViewModel.transferLog.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.LightGray
                ),
                title = {
                    Text(
                        text = "Transfer Log",
                        color = Color.Black
                    )
                },

                // Set back arrow to return the home screen
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Routes.homeScreen) }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .padding(start = 6.dp, end = 8.dp)
                                .size(40.dp)
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Transfer Summary",
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )

            // Display each transfer log
            transferLog.value.forEach { log ->
                Text(text = log, fontSize = 24.sp, modifier = Modifier.padding(16.dp))
            }
        }
    }
}