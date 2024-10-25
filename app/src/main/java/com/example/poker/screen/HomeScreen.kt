package com.example.poker.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.poker.R
import com.example.poker.route.Routes
import com.example.poker.ui.theme.PokerTheme

@Composable
fun HomeScreen(navController: NavHostController) {

    // Get the current activity
    val activity = (LocalContext.current as? Activity)

    // Handle back button press to go to the home screen
    BackHandler {
        activity?.finish() // Close the activity, returning to the device's home screen
    }
    BoxComponent(navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxComponent(navController: NavHostController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.LightGray
                ),
                title = {
                    Text(
                        text = "",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (context is Activity) {
                            context.finish()
                        }
                    } ) {
                        // Set back arrow to exit the app
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .padding(start = 6.dp, end = 8.dp)
                                .size(40.dp),
                        )
                    }
                })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main),
                contentDescription = "App Logo",
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(bottomStart = 35.dp, bottomEnd = 35.dp))
                    .align(Alignment.TopCenter)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                ButtonComponent("Start Game", navController, Routes.startGame)
                ButtonComponent("Add New Player",navController,Routes.addUserScreen)
                ButtonComponent("Player Balance", navController, Routes.playerBalance)
                ButtonComponent("History By Day", navController, Routes.historyByDay)
            }
        }
    }
}

@Composable
fun ButtonComponent(buttonText: String, navController: NavHostController, navigateTo: String) {
    var showDialog by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier
            .height(60.dp)
            .width(180.dp),
        onClick = {
            navController.navigate(navigateTo)
        },
        shape = RoundedCornerShape(15.dp),
        enabled = true,
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.DarkGray
        )
    ) {
        Text(
            text = buttonText,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameStartButtonPreview() {
    val navController = rememberNavController()
    PokerTheme {
        BoxComponent(navController)
    }
}