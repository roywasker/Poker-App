package com.example.poker.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {
            TextFieldComponent(viewModel)
            Spacer(modifier = Modifier.height(40.dp))
            ButtonForAddUserComponent("Add Player",onClick = viewModel::addUser)
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