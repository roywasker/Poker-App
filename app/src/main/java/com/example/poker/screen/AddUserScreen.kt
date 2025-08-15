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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.poker.data.AddUserViewModel
import com.example.poker.route.Routes

@Composable
fun AddUserScreen(navController: NavHostController, ) {
    val viewModel: AddUserViewModel = viewModel() // Get ViewModel instance
    viewModel.getPlayerList()

    BackHandler {
        navController.navigate(Routes.homeScreen)
    }

    AddUserComponent(navController,viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserComponent(navController: NavHostController, viewModel: AddUserViewModel) {
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
                // Back arrow to do the home screen
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
                })
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add new player",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.W500
                )
            }
            Spacer(modifier = Modifier.height(40.dp))

            TextFieldComponent(viewModel)
            Spacer(modifier = Modifier.height(40.dp))
            ButtonForAddUserComponent("Add Player",onClick = viewModel::addUser)
            Spacer(modifier = Modifier.height(40.dp))
            ChangeUserBalance("Change", viewModel = viewModel)
        }
    }

    // If view model set pop up massage, display it
    if (viewModel.massageDialog.value != null) {
        AlertDialog(
            onDismissRequest = { viewModel.massageDialog.value = null },
            text = { Text(viewModel.massageDialog.value!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.massageDialog.value = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun TextFieldComponent(viewModel: AddUserViewModel) {

    var userName by viewModel.userName
    OutlinedTextField(
        value = userName,
        onValueChange = {
            // Save the name in the view model
            viewModel.onUserNameChange(it)
        },
        label = { Text(text = "Enter name") },
        placeholder = { Text(text = "Enter name") }, // Display placeholder when empty
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = {
                viewModel.addUser() // Call the function to add the user when the "Send" button is pressed
            }
        ),
        shape = RoundedCornerShape(15.dp)
    )
}

@Composable
fun ButtonForAddUserComponent(buttonText: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .height(60.dp)
            .width(180.dp),
        onClick = {
            onClick()
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

@Composable
fun ChangeUserBalance(textForButton: String, viewModel: AddUserViewModel) {
    Spacer(modifier = Modifier.height(60.dp))
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Deduct money from a player",
            fontSize = 26.sp,
            fontWeight = FontWeight.W500
        )
    }
    Spacer(modifier = Modifier.height(40.dp))
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ){
        DropDown(viewModel)
        AmountFieldComponent(viewModel)
    }
    Spacer(modifier = Modifier.height(40.dp))
    ButtonForAddUserComponent(textForButton , viewModel::changeUserValue)
}

@Composable
fun AmountFieldComponent(viewModel: AddUserViewModel) {
    val money by viewModel.moneyChange
    OutlinedTextField(
        value = money,
        modifier = Modifier
            .width(80.dp)
            .height(50.dp),
        onValueChange = {
            viewModel.moneyChange.value = it
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(15.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun DropDown(viewModel: AddUserViewModel) {
    val list = viewModel.playerList // Assuming playerList is a simple list in the ViewModel
    var isExpanded by remember { mutableStateOf(false) }
    var localUserToChange by remember { viewModel.userToChange } // Local state for dropdown selection

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = localUserToChange, // Use the local state here
            onValueChange = {}, // Read-only, no need for value change logic
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .width(120.dp)
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            }
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            list.forEach { text ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = text,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        isExpanded = false
                        viewModel.userToChange.value = text
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}