package com.example.poker.util

/**
 * Validation utilities for user input
 */
object ValidationUtils {
    
    /**
     * Validate username according to business rules
     */
    fun validateUsername(username: String): ValidationResult {
        return when {
            username.isBlank() -> 
                ValidationResult.Error("Username cannot be empty")
            
            username.length < Constants.MIN_USERNAME_LENGTH -> 
                ValidationResult.Error("Username too short (minimum ${Constants.MIN_USERNAME_LENGTH} characters)")
            
            username.length > Constants.MAX_USERNAME_LENGTH -> 
                ValidationResult.Error("Username too long (maximum ${Constants.MAX_USERNAME_LENGTH} characters)")
            
            !username.matches(Regex(Constants.USERNAME_PATTERN)) -> 
                ValidationResult.Error("Username contains invalid characters. Only letters, numbers, spaces, hyphens and underscores allowed.")
            
            username.trim() != username ->
                ValidationResult.Error("Username cannot start or end with spaces")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate money amount
     */
    fun validateMoneyAmount(amount: String): ValidationResult {
        return when {
            amount.isBlank() -> 
                ValidationResult.Error("Amount cannot be empty")
            
            amount.toIntOrNull() == null -> 
                ValidationResult.Error("Please enter a valid number")
            
            amount.toInt() < 0 -> 
                ValidationResult.Error("Amount cannot be negative")
            
            amount.toInt() > 1_000_000 -> 
                ValidationResult.Error("Amount too large (maximum 1,000,000)")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate player count
     */
    fun validatePlayerCount(count: Int): ValidationResult {
        return when {
            count < Constants.MIN_PLAYERS -> 
                ValidationResult.Error("Minimum ${Constants.MIN_PLAYERS} players required")
            
            count > Constants.MAX_PLAYERS -> 
                ValidationResult.Error("Maximum ${Constants.MAX_PLAYERS} players allowed")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Sanitize user input to prevent injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input
            .trim()
            .replace(Regex("[<>\"'%;()&+]"), "") // Remove potentially dangerous characters
            .take(Constants.MAX_USERNAME_LENGTH) // Enforce max length
    }
}

/**
 * Result of validation operation
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isSuccess: Boolean
        get() = this is Success
    
    val errorMessage: String?
        get() = (this as? Error)?.message
}