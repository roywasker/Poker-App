package com.example.poker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poker.route.Routes
import com.example.poker.screen.AddUserScreen
import com.example.poker.screen.HomeScreen
import com.example.poker.screen.StartGameScreen
import com.example.poker.screen.TransferLogScreen
import com.example.poker.ui.theme.PokerTheme
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

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
            })
        }
    }
}
