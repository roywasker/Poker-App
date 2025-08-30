package com.example.poker.util

/**
 * Application-wide constants
 */
object Constants {
    
    // Player constraints
    const val MAX_PLAYERS = 9
    const val MIN_PLAYERS = 4

    // User validation
    const val MAX_USERNAME_LENGTH = 50
    const val MIN_USERNAME_LENGTH = 2
    const val USERNAME_PATTERN = "^[a-zA-Z0-9\\s-_]+$"
    
    // Firebase collections
    object Firebase {
        const val PLAYER_BALANCE_CHILD = "playerBalance"
    }
    
    // Error messages
    object ErrorMessages {
        const val GENERIC_ERROR = "An error occurred. Please try again."
    }
    
    // Logging tags
    const val TAG_PREFIX = "PokerApp_"
}