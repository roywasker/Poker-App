package com.example.poker.data.viewmodel

import android.util.Log
import com.example.poker.data.HistoryByDayViewModel
import com.example.poker.data.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryByDayViewModelTest {

    private lateinit var viewModel: HistoryByDayViewModel
    private lateinit var gameRepository: GameRepository

    @Before
    fun setup() {
        gameRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Verify initial state
        assertTrue(viewModel.dateList.isEmpty())
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
        assertEquals(Pair("Pick a date", ""), viewModel.dateSelected.value)
        assertNull(viewModel.messageDialog.value)
    }

    @Test
    fun `getDateList should load dates successfully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val mockDates = listOf(
            Pair("2024-01-01", "01-01-2024"),
            Pair("2024-01-02", "02-01-2024"),
            Pair("2024-01-03", "03-01-2024")
        )
        
        coEvery { gameRepository.getAllDates() } returns mockDates
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        viewModel.getDateList()
        
        // Verify dates were loaded
        assertEquals(mockDates, viewModel.dateList)
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getAllDates() }
    }

    @Test
    fun `getDateList should handle empty list`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        coEvery { gameRepository.getAllDates() } returns emptyList()
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        viewModel.getDateList()
        
        // Verify empty list is handled
        assertTrue(viewModel.dateList.isEmpty())
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getAllDates() }
    }

    @Test
    fun `getDateList should handle error gracefully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        coEvery { gameRepository.getAllDates() } throws Exception("Network error")
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        viewModel.getDateList()
        
        // Verify error is handled
        assertTrue(viewModel.dateList.isEmpty())
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getAllDates() }
    }

    @Test
    fun `getPlayerBalanceByDate should show message when no date selected`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Default date is "Pick a date"
        viewModel.getPlayerBalanceByDate()
        
        // Verify message is shown
        assertEquals("Please pick a date", viewModel.messageDialog.value)
        assertTrue(viewModel.playerList.value.isEmpty())
    }

    @Test
    fun `getPlayerBalanceByDate should load player balances successfully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val selectedDate = Pair("2024-01-01", "01-01-2024")
        val mockPlayerBalances = listOf(
            Pair("Player1", 100),
            Pair("Player2", -50),
            Pair("Player3", 200)
        )
        
        coEvery { gameRepository.getPlayerBalanceByDate("01-01-2024") } returns mockPlayerBalances
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        viewModel.dateSelected.value = selectedDate
        
        viewModel.getPlayerBalanceByDate()
        
        // Verify player balances were loaded
        assertEquals(mockPlayerBalances, viewModel.playerList.value)
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getPlayerBalanceByDate("01-01-2024") }
    }

    @Test
    fun `getPlayerBalanceByDate should handle empty player list`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val selectedDate = Pair("2024-01-01", "01-01-2024")
        
        coEvery { gameRepository.getPlayerBalanceByDate("01-01-2024") } returns emptyList()
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        viewModel.dateSelected.value = selectedDate
        
        viewModel.getPlayerBalanceByDate()
        
        // Verify empty list is handled
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getPlayerBalanceByDate("01-01-2024") }
    }

    @Test
    fun `getPlayerBalanceByDate should handle error gracefully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val selectedDate = Pair("2024-01-01", "01-01-2024")
        
        coEvery { gameRepository.getPlayerBalanceByDate("01-01-2024") } throws Exception("Database error")
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        viewModel.dateSelected.value = selectedDate
        
        viewModel.getPlayerBalanceByDate()
        
        // Verify error is handled
        assertFalse(viewModel.loading.value)
        
        coVerify { gameRepository.getPlayerBalanceByDate("01-01-2024") }
    }

    @Test
    fun `loading state should be set correctly during getDateList`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val mockDates = listOf(Pair("2024-01-01", "01-01-2024"))
        
        coEvery { gameRepository.getAllDates() } returns mockDates
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Loading should be false initially
        assertFalse(viewModel.loading.value)
        
        viewModel.getDateList()
        
        // Loading should be false after completion
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `loading state should be set correctly during getPlayerBalanceByDate`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val selectedDate = Pair("2024-01-01", "01-01-2024")
        val mockPlayerBalances = listOf(Pair("Player1", 100))
        
        coEvery { gameRepository.getPlayerBalanceByDate("01-01-2024") } returns mockPlayerBalances
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        viewModel.dateSelected.value = selectedDate
        
        // Loading should be false initially
        assertFalse(viewModel.loading.value)
        
        viewModel.getPlayerBalanceByDate()
        
        // Loading should be false after completion
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `clearMessageDialog should clear message`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Set a message
        viewModel.getPlayerBalanceByDate() // This will set "Please pick a date"
        assertNotNull(viewModel.messageDialog.value)
        
        // Clear the message
        viewModel.clearMessageDialog()
        
        // Verify message is cleared
        assertNull(viewModel.messageDialog.value)
    }

    @Test
    fun `dateSelected can be updated`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Initial value
        assertEquals(Pair("Pick a date", ""), viewModel.dateSelected.value)
        
        // Update selected date
        val newDate = Pair("2024-01-15", "15-01-2024")
        viewModel.dateSelected.value = newDate
        
        // Verify update
        assertEquals(newDate, viewModel.dateSelected.value)
    }

    @Test
    fun `multiple date selections should work correctly`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val date1 = Pair("2024-01-01", "01-01-2024")
        val date2 = Pair("2024-01-02", "02-01-2024")
        
        val playerBalances1 = listOf(Pair("Player1", 100))
        val playerBalances2 = listOf(Pair("Player1", 150))
        
        coEvery { gameRepository.getPlayerBalanceByDate("01-01-2024") } returns playerBalances1
        coEvery { gameRepository.getPlayerBalanceByDate("02-01-2024") } returns playerBalances2
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Select first date
        viewModel.dateSelected.value = date1
        viewModel.getPlayerBalanceByDate()
        assertEquals(playerBalances1, viewModel.playerList.value)
        
        // Select second date
        viewModel.dateSelected.value = date2
        viewModel.getPlayerBalanceByDate()
        assertEquals(playerBalances2, viewModel.playerList.value)
        
        coVerify { gameRepository.getPlayerBalanceByDate("01-01-2024") }
        coVerify { gameRepository.getPlayerBalanceByDate("02-01-2024") }
    }

    @Test
    fun `dateList should be cleared before loading new dates`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val firstDates = listOf(Pair("2024-01-01", "01-01-2024"))
        val secondDates = listOf(
            Pair("2024-02-01", "01-02-2024"),
            Pair("2024-02-02", "02-02-2024")
        )
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        // First call returns firstDates
        coEvery { gameRepository.getAllDates() } returns firstDates
        
        viewModel = HistoryByDayViewModel(gameRepository)
        
        // Load first set of dates
        viewModel.getDateList()
        assertEquals(firstDates, viewModel.dateList)
        
        // Update mock to return secondDates
        coEvery { gameRepository.getAllDates() } returns secondDates
        
        // Load second set of dates
        viewModel.getDateList()
        assertEquals(secondDates, viewModel.dateList)
    }
}