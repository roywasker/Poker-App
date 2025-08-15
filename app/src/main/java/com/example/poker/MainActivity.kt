package com.example.poker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poker.route.Routes
import com.example.poker.screen.AddUserScreen
import com.example.poker.screen.HistoryByDayScreen
import com.example.poker.screen.HomeScreen
import com.example.poker.screen.PlayerBalanceScreen
import com.example.poker.screen.StartGameScreen
import com.example.poker.screen.TransferLogScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "HomeScreen", builder = {
                composable(Routes.homeScreen){
                    HomeScreen(navController)
                }
                composable(Routes.addUserScreen){
                    AddUserScreen(navController)
                }
                composable(Routes.startGame){
                    StartGameScreen(navController)
                }
                composable(Routes.TransferLog){
                    TransferLogScreen(navController)
                }
                composable(Routes.playerBalance){
                    PlayerBalanceScreen(navController)
                }
                composable(Routes.historyByDay){
                    HistoryByDayScreen(navController)
                }
            })
        }
    }
}
