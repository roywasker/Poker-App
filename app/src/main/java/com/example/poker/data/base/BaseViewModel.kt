package com.example.poker.data.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.util.Constants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common functionality for all ViewModels
 */
abstract class BaseViewModel : ViewModel() {
    
    protected val TAG = "${Constants.TAG_PREFIX}${javaClass.simpleName}"
    
    // Loading state
    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    // Error/message dialog
    protected val _messageDialog = MutableStateFlow<String?>(null)
    val messageDialog: StateFlow<String?> = _messageDialog.asStateFlow()
    
    // Network error state with retry action
    protected val _networkError = MutableStateFlow<NetworkErrorState?>(null)
    val networkError: StateFlow<NetworkErrorState?> = _networkError.asStateFlow()
    
    data class NetworkErrorState(
        val message: String,
        val retryAction: suspend () -> Unit
    )
    
    // Coroutine exception handler
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception: ${throwable.message}", throwable)
        _loading.value = false
        _messageDialog.value = Constants.ErrorMessages.GENERIC_ERROR
    }
    
    /**
     * Execute a suspending function with loading state and error handling
     */
    protected fun launchWithLoading(
        onError: (Exception) -> Unit = { e ->
            Log.e(TAG, "Error: ${e.message}", e)
            _messageDialog.value = Constants.ErrorMessages.GENERIC_ERROR
        },
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            _loading.value = true
            try {
                block()
                // Clear any existing network error on successful completion
                _networkError.value = null
            } catch (e: Exception) {
                onError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Clear the message dialog
     */
    fun clearMessageDialog() {
        _messageDialog.value = null
    }
    
    /**
     * Show a message to the user
     */
    protected fun showMessage(message: String) {
        _messageDialog.value = message
    }
    
    /**
     * Show network error with retry option
     */
    protected fun showNetworkError(message: String, retryAction: suspend () -> Unit) {
        _networkError.value = NetworkErrorState(message, retryAction)
    }
    
    /**
     * Clear network error
     */
    fun clearNetworkError() {
        _networkError.value = null
    }
    
    /**
     * Retry the last failed network operation
     */
    fun retryNetworkOperation() {
        val error = _networkError.value ?: return
        viewModelScope.launch(exceptionHandler) {
            _loading.value = true
            _networkError.value = null
            try {
                error.retryAction()
            } finally {
                _loading.value = false
            }
        }
    }
}