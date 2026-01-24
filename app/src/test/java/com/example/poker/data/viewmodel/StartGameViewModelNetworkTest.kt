package com.example.poker.data.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.poker.data.StartGameViewModel
import com.example.poker.data.repository.TransferLogRepository
import com.example.poker.data.database.PlayerSessionRepository
import com.example.poker.data.database.SessionData
import com.example.poker.data.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StartGameViewModelNetworkTest {

    private lateinit var viewModel: StartGameViewModel
    private lateinit var sessionRepository: PlayerSessionRepository
    private lateinit var gameRepository: GameRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        gameRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        // Mock TransferLogRepository
        mockkObject(TransferLogRepository)
        every { TransferLogRepository.cleanTransferLog() } returns Unit
        every { TransferLogRepository.addLogEntry(any()) } returns Unit
        
        // Default mock: no active session
        coEvery { sessionRepository.hasActiveSession() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(TransferLogRepository)
    }

    private fun createViewModel(): StartGameViewModel {
        return StartGameViewModel(sessionRepository, gameRepository)
    }

    @Test
    fun `getPlayerListToStartGame should handle timeout and show network error`() = runTest {
        coEvery { gameRepository.getPlayersForStartGame() } coAnswers {
            delay(4000) // Delay longer than 3 seconds timeout
            listOf("Player1", "Player2")
        }
        
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        viewModel.getPlayerListToStartGame()
        
        // Advance time to trigger timeout (3 seconds)
        advanceTimeBy(3001)
        advanceUntilIdle()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
        
        // Player list should be empty due to timeout
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `finishGameButton should handle timeout when updating balance`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "50"
        
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } throws Exception("Request timeout")
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        val result = viewModel.finishGameButton()
        
        // Should return true as the balance calculation passes
        assertTrue(result)
        
        // Wait for async operation to complete
        advanceUntilIdle()
        
        // Should show error message due to timeout
        assertEquals("Failed to update game data. Please try again.", viewModel.messageDialog.value)
    }

    @Test
    fun `network error retry should work correctly for getPlayerListToStartGame`() = runTest {
        var callCount = 0
        coEvery { gameRepository.getPlayersForStartGame() } coAnswers {
            callCount++
            if (callCount == 1) {
                delay(4000) // First call times out
                listOf<String>()
            } else {
                listOf("Player1", "Player2") // Second call succeeds
            }
        }
        
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        // First call - will timeout
        viewModel.getPlayerListToStartGame()
        advanceTimeBy(3001)
        advanceUntilIdle()
        assertNotNull(viewModel.networkError.value)
        
        // Retry
        viewModel.retryNetworkOperation()
        advanceUntilIdle()
        
        // Should now have players
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `clearNetworkError should clear error state`() = runTest {
        coEvery { gameRepository.getPlayersForStartGame() } coAnswers {
            delay(4000) // Simulate timeout
            listOf<String>()
        }
        
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        viewModel.getPlayerListToStartGame()
        advanceTimeBy(3001)
        advanceUntilIdle()
        assertNotNull(viewModel.networkError.value)
        
        viewModel.clearNetworkError()
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `timeout exception should be properly identified`() = runTest {
        coEvery { gameRepository.getPlayersForStartGame() } coAnswers {
            delay(4000) // This will timeout after 3 seconds
            listOf<String>()
        }
        
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        viewModel.getPlayerListToStartGame()
        advanceTimeBy(3001)
        advanceUntilIdle()
        
        // Should show network error with timeout message
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `successful network call should clear any existing network error`() = runTest {
        // First set up a timeout error
        coEvery { gameRepository.getPlayersForStartGame() } coAnswers {
            delay(4000)
            listOf<String>()
        }
        viewModel = createViewModel()
        advanceUntilIdle() // Let init complete
        
        viewModel.getPlayerListToStartGame()
        advanceTimeBy(3001)
        advanceUntilIdle()
        assertNotNull(viewModel.networkError.value)
        
        // Now make a successful call
        coEvery { gameRepository.getPlayersForStartGame() } returns listOf("Player1", "Player2")
        viewModel.getPlayerListToStartGame()
        advanceUntilIdle()
        
        // Network error should be cleared
        assertNull(viewModel.networkError.value)
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
    }
}