package com.example.poker.data.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.poker.data.HistoryByDayViewModel
import com.example.poker.data.repository.GameRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HistoryByDayViewModelNetworkTest {

    private lateinit var viewModel: HistoryByDayViewModel
    private lateinit var gameRepository: GameRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        gameRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getDateList should handle timeout and show network error`() = runTest {
        coEvery { gameRepository.getAllDates() } throws Exception("Connection timeout")
        
        viewModel.getDateList()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
        
        // Date list should be empty due to timeout
        assertTrue(viewModel.dateList.isEmpty())
    }

    @Test
    fun `getPlayerBalanceByDate should handle timeout and show network error`() = runTest {
        viewModel.dateSelected.value = Pair("01/01/2024", "2024-01-01")
        
        coEvery { gameRepository.getPlayerBalanceByDate("2024-01-01") } throws Exception("Request timeout")
        
        viewModel.getPlayerBalanceByDate()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
        
        // Player list should be empty due to timeout
        assertTrue(viewModel.playerList.value.isEmpty())
    }

    @Test
    fun `getPlayerBalanceByDate should show message when no date selected`() = runTest {
        viewModel.dateSelected = mutableStateOf(Pair("Pick a date", ""))
        
        viewModel.getPlayerBalanceByDate()
        
        // Should show message dialog, not network error
        assertEquals("Please pick a date", viewModel.messageDialog.value)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `timeout exception should be properly identified for getDateList`() = runTest {
        coEvery { gameRepository.getAllDates() } throws Exception("Operation timeout")
        
        viewModel.getDateList()
        
        // Should show network error with timeout message
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `timeout exception should be properly identified for getPlayerBalanceByDate`() = runTest {
        viewModel.dateSelected.value = Pair("01/01/2024", "2024-01-01")
        coEvery { gameRepository.getPlayerBalanceByDate("2024-01-01") } throws Exception("Request timeout")
        
        viewModel.getPlayerBalanceByDate()
        
        // Should show network error with timeout message
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `network error retry should work correctly for getDateList`() = runTest {
        var callCount = 0
        coEvery { gameRepository.getAllDates() } answers {
            callCount++
            if (callCount == 1) {
                throw Exception("Connection timeout")
            } else {
                listOf("2024-01-01" to "01/01/2024", "2024-01-02" to "02/01/2024") // Second call succeeds
            }
        }
        
        // First call - will timeout
        viewModel.getDateList()
        assertNotNull(viewModel.networkError.value)
        
        // Retry
        viewModel.retryNetworkOperation()
        
        // Should now have dates
        assertEquals(2, viewModel.dateList.size)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `network error retry should work correctly for getPlayerBalanceByDate`() = runTest {
        viewModel.dateSelected.value = Pair("01/01/2024", "2024-01-01")
        
        var callCount = 0
        coEvery { gameRepository.getPlayerBalanceByDate("2024-01-01") } answers {
            callCount++
            if (callCount == 1) {
                throw Exception("Request timeout")
            } else {
                listOf("Player1" to 100, "Player2" to 200) // Second call succeeds
            }
        }
        
        // First call - will timeout
        viewModel.getPlayerBalanceByDate()
        assertNotNull(viewModel.networkError.value)
        
        // Retry
        viewModel.retryNetworkOperation()
        
        // Should now have players
        assertEquals(listOf("Player1" to 100, "Player2" to 200), viewModel.playerList.value)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `clearNetworkError should clear error state`() = runTest {
        coEvery { gameRepository.getAllDates() } throws Exception("Connection timeout")
        
        viewModel.getDateList()
        assertNotNull(viewModel.networkError.value)
        
        viewModel.clearNetworkError()
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `successful network call should clear any existing network error`() = runTest {
        // First set up a timeout error
        coEvery { gameRepository.getAllDates() } throws Exception("Request timeout")
        viewModel.getDateList()
        assertNotNull(viewModel.networkError.value)
        
        // Now make a successful call
        coEvery { gameRepository.getAllDates() } returns listOf("2024-01-01" to "01/01/2024")
        viewModel.getDateList()
        
        // Network error should be cleared
        assertNull(viewModel.networkError.value)
        assertEquals(1, viewModel.dateList.size)
    }

    @Test
    fun `non-timeout exception should show regular error message for getDateList`() = runTest {
        coEvery { gameRepository.getAllDates() } throws Exception("Database error")
        
        viewModel.getDateList()
        
        // Should show regular message, not network error
        assertEquals("Failed to load dates. Please try again.", viewModel.messageDialog.value)
    }

    @Test
    fun `non-timeout exception should show regular error message for getPlayerBalanceByDate`() = runTest {
        viewModel.dateSelected.value = Pair("01/01/2024", "2024-01-01")
        coEvery { gameRepository.getPlayerBalanceByDate("2024-01-01") } throws Exception("Database error")
        
        viewModel.getPlayerBalanceByDate()
        
        // Should show regular message, not network error
        assertEquals("Failed to load player balances. Please try again.", viewModel.messageDialog.value)
    }

    @Test
    fun `multiple rapid timeout errors should only show one error`() = runTest {
        coEvery { gameRepository.getAllDates() } throws Exception("Request timeout")
        
        // Make multiple rapid calls
        viewModel.getDateList()
        viewModel.getDateList()
        viewModel.getDateList()
        
        // Should still have only one error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
    }
}