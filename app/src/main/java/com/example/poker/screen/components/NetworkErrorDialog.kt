package com.example.poker.screen.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.poker.data.base.BaseViewModel

/**
 * Reusable dialog for network errors with retry functionality
 */
@Composable
fun NetworkErrorDialog(
    errorState: BaseViewModel.NetworkErrorState?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    errorState?.let { error ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Text(
                    text = "Connection Error",
                    color = Color.Red
                )
            },
            text = { 
                Text(text = error.message)
            },
            confirmButton = {
                TextButton(
                    onClick = onRetry) {
                    Text("Try Again",
                        color = Color.DarkGray)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel",
                        color = Color.DarkGray)
                }
            }
        )
    }
}