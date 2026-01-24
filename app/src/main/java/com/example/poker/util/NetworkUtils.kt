package com.example.poker.util

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Network utilities for handling timeouts and retries
 */
object NetworkUtils {
    
    // Default timeout for network operations (3 seconds)
    const val DEFAULT_TIMEOUT_MS = 3000L
    
    /**
     * Result wrapper for network operations
     */
    sealed class NetworkResult<out T> {
        data class Success<T>(val data: T) : NetworkResult<T>()
        data class Error(val exception: Exception, val isTimeout: Boolean = false) : NetworkResult<Nothing>()
        object Loading : NetworkResult<Nothing>()
    }
    
    /**
     * Execute a network call with timeout
     */
    suspend fun <T> withNetworkTimeout(
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        block: suspend () -> T
    ): NetworkResult<T> {
        return try {
            withTimeout(timeoutMs) {
                NetworkResult.Success(block())
            }
        } catch (e: TimeoutCancellationException) {
            NetworkResult.Error(
                exception = Exception("Connection timeout. Please check your internet connection."),
                isTimeout = true
            )
        } catch (e: Exception) {
            NetworkResult.Error(
                exception = e,
                isTimeout = false
            )
        }
    }
    
    /**
     * Retry configuration
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val delayMs: Long = 1000L,
        val timeoutMs: Long = DEFAULT_TIMEOUT_MS
    )
}