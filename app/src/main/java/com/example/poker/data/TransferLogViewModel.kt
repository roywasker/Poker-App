package com.example.poker.data

import androidx.lifecycle.ViewModel
import com.example.poker.data.repository.TransferLogRepository

class TransferLogViewModel: ViewModel() {

    // Observe the shared transfer log list from the repository
    val transferLog = TransferLogRepository.transferLogList
}