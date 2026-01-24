package com.example.poker.data.database

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import java.util.UUID

class PlayerSessionRepository(private val dao: PlayerSessionDao) {
    suspend fun hasActiveSession(): Boolean = dao.hasActiveSession()
    
    suspend fun saveSession(
        numOfRows: Int,
        playerNames: Array<MutableState<String>>,
        buyAmounts: Array<MutableState<String>>,
        returnAmounts: Array<MutableState<String>>
    ) {
        val session = PlayerSessionEntity(
            id = 0, // Single session
            sessionId = UUID.randomUUID().toString(),
            numOfRows = numOfRows,
            playerNames = arrayToJson(playerNames),
            buyAmounts = arrayToJson(buyAmounts),
            returnAmounts = arrayToJson(returnAmounts)
        )
        dao.saveSession(session)
    }
    
    suspend fun loadSession(): SessionData? {
        val session = dao.getCurrentSessionOnce() ?: return null
        
        return SessionData(
            numOfRows = session.numOfRows,
            playerNames = jsonToMutableStateArray(session.playerNames),
            buyAmounts = jsonToMutableStateArray(session.buyAmounts),
            returnAmounts = jsonToMutableStateArray(session.returnAmounts)
        )
    }
    
    suspend fun clearSession() = dao.clearSession()
    
    private fun arrayToJson(array: Array<MutableState<String>>): String {
        return try {
            val jsonArray = JSONArray()
            array.forEach { state ->
                jsonArray.put(state.value)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            // If JSON creation fails, return empty JSON array
            "[]"
        }
    }
    
    private fun jsonToMutableStateArray(jsonString: String): Array<MutableState<String>> {
        return try {
            val jsonArray = JSONArray(jsonString)
            Array(9) { index ->
                if (index < jsonArray.length()) {
                    mutableStateOf(jsonArray.getString(index))
                } else {
                    mutableStateOf("")
                }
            }
        } catch (e: Exception) {
            // If JSON parsing fails, return array of empty strings
            Array(9) { mutableStateOf("") }
        }
    }
}

data class SessionData(
    val numOfRows: Int,
    val playerNames: Array<MutableState<String>>,
    val buyAmounts: Array<MutableState<String>>,
    val returnAmounts: Array<MutableState<String>>
)