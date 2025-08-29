package com.example.poker.screen

import android.annotation.SuppressLint
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.poker.R
import com.example.poker.route.Routes

@SuppressLint("ContextCastToActivity")
@Composable
fun HomeScreen(navController: NavHostController) {

    // Get the current activity
    val activity = (LocalContext.current as? Activity)

    // Handle back button press to close the app properly
    BackHandler {
        activity?.finishAffinity() // Close the app properly without memory leaks
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
                            context.finishAffinity()
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
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                ButtonComponent(context.getString(R.string.start_game), navController, Routes.startGame)
                ButtonComponent(context.getString(R.string.add_new_player),navController,Routes.addUserScreen)
                ButtonComponent(context.getString(R.string.player_balance), navController, Routes.playerBalance)
                ButtonComponent(context.getString(R.string.history_by_day), navController, Routes.historyByDay)
                ButtonComponent(context.getString(R.string.player_statistics), navController, Routes.playerStatistics)
            }
        }
    }
}

@Composable
fun ButtonComponent(buttonText: String, navController: NavHostController, navigateTo: String) {
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