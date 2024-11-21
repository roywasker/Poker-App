package com.example.poker.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

            Spacer(modifier = Modifier.height(40.dp))
            ButtonComponent("Share on WhatsApp" , transferLog)
        }
    }
}

@Composable
fun ButtonComponent(buttonText: String, transferLog: State<List<String>>) {

    val context = LocalContext.current

    Button(
        modifier = Modifier
            .height(60.dp)
            .width(300.dp),
        onClick = {
            ShareSummeryOnWhatsApp(context, transferLog)
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

/**
 * Share the Transfer Summary to WhatsApp
 */
fun ShareSummeryOnWhatsApp(context: Context, transferLog: State<List<String>>) {

    // Access the value of the state
    val list: List<String> = transferLog.value

    // Convert List<String> to a single String
    val resultLog: String = list.joinToString(separator = "\n") // Customize the separator as needed

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, resultLog)

        // Ensure it opens WhatsApp
        setPackage("com.whatsapp")
    }

    try {
        // Start the activity
        context.startActivity(intent)
    } catch (e: Exception) {
        // Show a toast if WhatsApp is not installed
        Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
    }
}