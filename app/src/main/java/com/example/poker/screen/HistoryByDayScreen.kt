package com.example.poker.screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.poker.data.HistoryByDayViewModel
import com.example.poker.route.Routes


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryByDayScreen(navController: NavHostController) {
    // ViewModel instance
    val historyByDayViewModel: HistoryByDayViewModel = viewModel()

    val loading by historyByDayViewModel.loading
    if (loading) {
        LoadingScreen()// Display a loading screen
    } else {
        HistoryByDayComponent(navController, historyByDayViewModel)
    }

    BackHandler {
        navController.navigate(Routes.homeScreen)
    }

    LaunchedEffect(Unit) {
        if (!loading) {
            historyByDayViewModel.getDateList()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryByDayComponent(navController: NavHostController, historyByDayViewModel: HistoryByDayViewModel) {
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
            Spacer(modifier = Modifier.height(32.dp))
            HistoryByDayComponent(viewModel = historyByDayViewModel)
            Spacer(modifier = Modifier.height(22.dp))
            ShowPlayerBalanceComponent(viewModel = historyByDayViewModel)
        }
    }

    // If view model set pop up massage, display it
    if (historyByDayViewModel.massageDialog.value != null) {
        AlertDialog(
            onDismissRequest = { historyByDayViewModel.massageDialog.value = null },
            text = { Text(historyByDayViewModel.massageDialog.value!!) },
            confirmButton = {
                TextButton(onClick = { historyByDayViewModel.massageDialog.value = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryByDayComponent(viewModel: HistoryByDayViewModel) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ){
        DropDownHistory(viewModel = viewModel)
        Spacer(modifier = Modifier.width(16.dp))
        ButtonShowDataComponent("Show Player Data", viewModel)
    }
}

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownHistory(viewModel: HistoryByDayViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Keep dates list in remember to preserve state across recompositions
    val datesList by remember(viewModel.dateList) { 
        mutableStateOf(viewModel.dateList.asReversed())
    }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it }
    ) {
        OutlinedTextField(
            value = viewModel.dateSelected.value.first,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .width(160.dp)
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            datesList.forEach { date ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = date.first,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        viewModel.dateSelected.value = date
                        isExpanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ButtonShowDataComponent(
    buttonText: String,
    viewModel: HistoryByDayViewModel
) {
    Button(
        modifier = Modifier
            .height(60.dp)
            .width(160.dp),
        onClick = {
            viewModel.getPlayerBalanceByDate()

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
            fontSize = 14.sp
        )
    }
}

@Composable
fun ShowPlayerBalanceComponent(viewModel: HistoryByDayViewModel){
    val playerBalanceList: List<Pair<String, Int>> = viewModel.playerList

    if (playerBalanceList.isEmpty()){
        return
    }
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
    for (pair in playerBalanceList){
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            BasicText(text = pair.first, modifier = Modifier.weight(1f), style = TextStyle(fontSize = 18.sp))
            BasicText(text = pair.second.toString(), modifier = Modifier.weight(1f), style = TextStyle(fontSize = 18.sp))
        }
        Spacer(modifier = Modifier.height(6.dp)) // Add some spacing between rows
    }
}