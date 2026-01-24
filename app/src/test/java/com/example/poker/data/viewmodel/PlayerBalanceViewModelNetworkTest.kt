package com.example.poker.data.viewmodel

import android.util.Log
import com.example.poker.data.PlayerBalanceViewModel
import com.example.poker.data.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerBalanceViewModelNetworkTest {

    private lateinit var viewModel: PlayerBalanceViewModel
    private lateinit var playerRepository: PlayerRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        playerRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getPlayerBalance should handle timeout and show network error`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Request timeout")
        
        viewModel.getPlayerBalance()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
        
        // Player list should be empty due to timeout
        assertTrue(viewModel.playerList.value.isEmpty())
        assertTrue(viewModel.filteredPlayerList.value.isEmpty())
    }

    @Test
    fun `timeout exception should be properly identified`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Connection timeout")
        
        viewModel.getPlayerBalance()
        
        // Should show network error with timeout message
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `network error retry should work correctly`() = runTest {
        var callCount = 0
        coEvery { playerRepository.getAllPlayers() } answers {
            callCount++
            if (callCount == 1) {
                throw Exception("Request timeout")
            } else {
                listOf("Player1" to 100, "Player2" to 200) // Second call succeeds
            }
        }
        
        // First call - will timeout
        viewModel.getPlayerBalance()
        assertNotNull(viewModel.networkError.value)
        
        // Retry
        viewModel.retryNetworkOperation()
        
        // Should now have players
        assertEquals(listOf("Player1" to 100, "Player2" to 200), viewModel.playerList.value)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `clearNetworkError should clear error state`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Connection timeout")
        
        viewModel.getPlayerBalance()
        assertNotNull(viewModel.networkError.value)
        
        viewModel.clearNetworkError()
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `successful network call should clear any existing network error`() = runTest {
        // First set up a timeout error
        coEvery { playerRepository.getAllPlayers() } throws Exception("Connection timeout")
        viewModel.getPlayerBalance()
        assertNotNull(viewModel.networkError.value)
        
        // Now make a successful call
        coEvery { playerRepository.getAllPlayers() } returns listOf("Player1" to 100, "Player2" to 200)
        viewModel.getPlayerBalance()
        
        // Network error should be cleared
        assertNull(viewModel.networkError.value)
        assertEquals(listOf("Player1" to 100, "Player2" to 200), viewModel.playerList.value)
    }

    @Test
    fun `refresh should handle timeout correctly`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Request timeout")
        
        viewModel.refresh()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `network error should preserve retry action for later use`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Connection timeout")
        
        viewModel.getPlayerBalance()
        
        val networkError = viewModel.networkError.value
        assertNotNull(networkError)
        assertNotNull(networkError?.retryAction)
        
        // Later, when network is restored
        coEvery { playerRepository.getAllPlayers() } returns listOf("Player1" to 100)
        
        // Execute the stored retry action
        viewModel.retryNetworkOperation()
        
        assertEquals(listOf("Player1" to 100), viewModel.playerList.value)
    }

    @Test
    fun `multiple rapid timeout errors should only show one error`() = runTest {
        coEvery { playerRepository.getAllPlayers() } throws Exception("Request timeout")
        
        // Make multiple rapid calls
        viewModel.getPlayerBalance()
        viewModel.refresh()
        viewModel.getPlayerBalance()
        
        // Should still have only one error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }
}