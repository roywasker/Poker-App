package com.example.poker.data.base

import android.util.Log
import com.example.poker.util.Constants
import com.example.poker.util.NetworkUtils
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Base repository providing common Firebase operations with timeout handling
 */
abstract class BaseRepository {
    
    protected val TAG = "${Constants.TAG_PREFIX}${javaClass.simpleName}"
    
    /**
     * Execute a Firebase operation safely with error handling and timeout
     */
    protected suspend fun <T> safeFirebaseCall(
        defaultValue: T,
        timeoutMs: Long = NetworkUtils.DEFAULT_TIMEOUT_MS,
        call: suspend () -> T
    ): T {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(timeoutMs) {
                    call()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Firebase operation timed out after ${timeoutMs}ms", e)
                defaultValue
            } catch (e: Exception) {
                Log.e(TAG, "Firebase operation failed", e)
                defaultValue
            }
        }
    }
    
    /**
     * Safe extraction of String value from DataSnapshot
     */
    protected fun DataSnapshot.getStringOrNull(key: String): String? {
        return try {
            child(key).getValue(String::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get string for key: $key", e)
            null
        }
    }
    
    /**
     * Safe extraction of Int value from DataSnapshot
     */
    protected fun DataSnapshot.getIntOrDefault(key: String, defaultValue: Int = 0): Int {
        return try {
            child(key).getValue(Int::class.java) ?: defaultValue
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get int for key: $key", e)
            defaultValue
        }
    }
    
    /**
     * Convert DataSnapshot children to list with mapping
     */
    protected fun <T> DataSnapshot.childrenToList(mapper: (DataSnapshot) -> T?): List<T> {
        return children.mapNotNull { snapshot ->
            try {
                mapper(snapshot)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to map snapshot: ${snapshot.key}", e)
                null
            }
        }
    }
}