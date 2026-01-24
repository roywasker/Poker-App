package com.example.poker.screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.poker.data.StartGameViewModel
import com.example.poker.route.Routes
import com.example.poker.screen.components.NetworkErrorDialog

const val maxPlayer = 9
const val minPlayer = 4

@SuppressLint("ContextCastToActivity")
@Composable
fun StartGameScreen(
    navController: NavHostController,
    viewModel: StartGameViewModel = hiltViewModel()
    ) {
    val loading by viewModel.loading.collectAsState()
    if (loading) {
        LoadingScreen()// Display a loading screen
    } else {
        StartGameComponent(navController,viewModel)
    }

    LaunchedEffect(Unit) {
        if (!loading) {
            viewModel.getPlayerListToStartGame()
        }
    }
    BackHandler {
        navController.navigate(Routes.homeScreen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartGameComponent(navController: NavHostController,viewModel: StartGameViewModel) {
    var numOfRows by viewModel.numOfRows
    val scrollState = rememberScrollState()
    
    // Stabilize navigation lambda to prevent recreation on every recomposition
    val navigateToHome = remember { { navController.navigate(Routes.homeScreen) } }
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
                // Set back arrow to go the home screen
                navigationIcon = {
                    IconButton(onClick = navigateToHome) {
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
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState), // Set the screen scrollable up and down
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top)
        {
            Spacer(modifier = Modifier.height(16.dp))
            TitleTextComponent()

            // Display row by default or user chose
            for (i  in 0..<numOfRows){
                // Use key to prevent unnecessary recompositions when list changes
                key(i) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PlayerDataComponent(index = i, viewModel= viewModel)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ){
                // Allow to add row only if it does not exceed the limit
                if (numOfRows < maxPlayer) {
                    AddRowButtonComponent(
                        onAddRow = {
                            numOfRows++
                        }
                    )
                }

                // Allow to delete row only if it does not exceed the limit
                if (numOfRows > minPlayer) {
                    RemoveRowButtonComponent(
                        onRemoveRow = {
                            numOfRows--
                            viewModel.removeRow(numOfRows)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val onFinishGame = remember { {
                val status = viewModel.finishGameButton()
                // If the function in view model finis successful move to transfer screen
                if (status) {
                    navController.navigate(route = Routes.TransferLog)
                }
            } }
            
            ButtonComponent(
                buttonText = "Finish Game",
                onClick = onFinishGame)

            // Show clear session button if there's an active session
            val hasActiveSession by viewModel.hasActiveSession.collectAsState()
            if (hasActiveSession) {
                Spacer(modifier = Modifier.height(12.dp))
                val onClearSession = remember { { viewModel.clearSavedSession() } }
                ButtonComponent(
                    buttonText = "Clear Session",
                    onClick = onClearSession
                )
            }
        }
    }

    // If view model set pop up message, display it
    val messageDialog by viewModel.messageDialog.collectAsState()
    messageDialog?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessageDialog() },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessageDialog() }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Show network error dialog with retry option
    val networkError by viewModel.networkError.collectAsState()
    NetworkErrorDialog(
        errorState = networkError,
        onRetry = { viewModel.retryNetworkOperation() },
        onDismiss = { 
            viewModel.clearNetworkError()
            navController.navigate(Routes.homeScreen)
        }
    )
}

@Composable
fun TitleTextComponent(){
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        BasicText(
            text = "Name",
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 18.sp) // Set font size using TextStyle
        )
        BasicText(
            text = "Buy",
            modifier = Modifier.weight(1f),
            style = TextStyle(fontSize = 18.sp) // Set font size using TextStyle
        )
        BasicText(
            text = "Return",
            modifier = Modifier.weight(0.5f),
            style = TextStyle(fontSize = 18.sp) // Set font size using TextStyle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDown(indexInArray: Int, viewModel: StartGameViewModel){
    val list by viewModel.playerList.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    // Use the current value from viewModel directly to avoid stale state
    val selectedPlayer = viewModel.nameOfPlayerArray[indexInArray].value

    ExposedDropdownMenuBox(expanded = isExpanded,
        onExpandedChange = {isExpanded=!isExpanded}) {
        OutlinedTextField(value = selectedPlayer ,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .width(120.dp)
                .height(50.dp),
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            }
        )
        ExposedDropdownMenu(expanded =isExpanded , onDismissRequest ={isExpanded = false} ) {
            list.forEachIndexed{ index , text ->
                DropdownMenuItem(
                    text = { Text(text = text,
                        fontSize = 14.sp)},
                    onClick ={
                        // Create stable lambda with remember
                        val currentSelected = viewModel.nameOfPlayerArray[indexInArray].value
                        // If user enter correct player name , remove this name from list
                        if (currentSelected != "") {
                            viewModel.removePlayerFromList(currentSelected)
                        }

                        val newSelectedPlayer = list[index]
                        viewModel.nameOfPlayerArray[indexInArray].value = newSelectedPlayer
                        isExpanded = false
                        viewModel.addPlayerToList(newSelectedPlayer)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun PlayerDataComponent(index: Int, viewModel: StartGameViewModel) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ){
        Spacer(modifier = Modifier.width(14.dp))
        DropDown(indexInArray = index, viewModel = viewModel)
        Spacer(modifier = Modifier.width(26.dp))
        BuyFieldComponent(index,viewModel)
        Spacer(modifier = Modifier.width(34.dp))
        ReturnFieldComponent(index,viewModel)
    }
}


@Composable
fun BuyFieldComponent(index: Int, viewModel: StartGameViewModel) {
    val money by viewModel.buyMoneyArray[index]
    // Stabilize the onValueChange lambda
    val onValueChange = remember(index) { { value: String ->
        viewModel.buyMoneyArray[index].value = value
        viewModel.onDataChanged() // Save session when data changes
    } }
    
    OutlinedTextField(
        value = money,
        modifier = Modifier
            .width(80.dp)
            .height(50.dp),
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
fun ReturnFieldComponent(index: Int, viewModel: StartGameViewModel) {
    val money by viewModel.returnMoneyArray[index]
    // Stabilize the onValueChange lambda
    val onValueChange = remember(index) { { value: String ->
        viewModel.returnMoneyArray[index].value = value
        viewModel.onDataChanged() // Save session when data changes
    } }
    
    OutlinedTextField(
        value = money,
        modifier = Modifier
            .width(80.dp)
            .height(50.dp),
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(15.dp)
    )
}


@Composable
fun AddRowButtonComponent(onAddRow: () -> Unit){
    Button(
        onClick = { onAddRow() },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.DarkGray
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add, // Use the default "Add" icon
            contentDescription = "Add Row"
        )
    }
}

@Composable
fun RemoveRowButtonComponent(onRemoveRow: () -> Unit) {
    Button(
        onClick = { onRemoveRow() },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.DarkGray
        )
    ) {
        Icon(
            imageVector = Icons.Default.Delete, // Use the default "Delete" icon
            contentDescription = "Remove Row"
        )
    }
}

@Composable
fun ButtonComponent(
    buttonText: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(60.dp)
            .width(180.dp),
        onClick =onClick,
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

@Composable
fun LoadingScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            color = Color.DarkGray
        )
    }
}