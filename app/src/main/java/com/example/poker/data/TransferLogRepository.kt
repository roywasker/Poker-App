package com.example.poker.data

import androidx.lifecycle.MutableLiveData

object TransferLogRepository {
    val transferLogList = MutableLiveData<List<String>>(mutableListOf())

    // Function to add log entries
    fun addLogEntry(entry: String) {
        val currentList = transferLogList.value?.toMutableList() ?: mutableListOf()
        currentList.add(entry)
        transferLogList.value = currentList
    }
}
