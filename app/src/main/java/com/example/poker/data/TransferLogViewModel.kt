package com.example.poker.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class TransferLogViewModel: ViewModel() {

    var loading = mutableStateOf(false)
    val transferLog = mutableListOf<String>()


}