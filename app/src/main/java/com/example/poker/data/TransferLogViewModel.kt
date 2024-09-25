package com.example.poker.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class TransferLogViewModel: ViewModel() {

    // Observe the shared transfer log list from the repository
    val transferLog = TransferLogRepository.transferLogList
}