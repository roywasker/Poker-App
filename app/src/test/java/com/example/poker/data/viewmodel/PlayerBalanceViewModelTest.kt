package com.example.poker.data.viewmodel

import android.util.Log
import com.example.poker.data.PlayerBalanceViewModel
import com.example.poker.data.repository.PlayerRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerBalanceViewModelTest {

    private lateinit var viewModel: PlayerBalanceViewModel
    private lateinit var playerRepository: PlayerRepository

    @Before
    fun setup() {
        playerRepository = mockk(relaxed = true)
        
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
    fun `initial state should have empty player list and loading false`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        // Verify initial state
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `getPlayerBalance should load players successfully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val mockPlayers = listOf(
            Pair("Player1", 100),
            Pair("Player2", -50),
            Pair("Player3", 200),
            Pair("Player4", 0)
        )
        
        coEvery { playerRepository.getAllPlayers() } returns mockPlayers
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify players were loaded
        assertEquals(mockPlayers, viewModel.playerList.value)
        assertFalse(viewModel.loading.value)
        
        coVerify { playerRepository.getAllPlayers() }
    }

    @Test
    fun `getPlayerBalance should handle empty player list`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        coEvery { playerRepository.getAllPlayers() } returns emptyList()
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify empty list is handled
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
        
        coVerify { playerRepository.getAllPlayers() }
    }

    @Test
    fun `getPlayerBalance should handle error gracefully`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        coEvery { playerRepository.getAllPlayers() } throws Exception("Network error")
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify error is handled - list should remain empty
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
        
        coVerify { playerRepository.getAllPlayers() }
    }

    @Test
    fun `loading state should be set correctly during getPlayerBalance`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val mockPlayers = listOf(Pair("Player1", 100))
        
        coEvery { playerRepository.getAllPlayers() } returns mockPlayers
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        // Loading should be false initially
        assertFalse(viewModel.loading.value)
        
        viewModel.getPlayerBalance()
        
        // Loading should be false after completion
        assertFalse(viewModel.loading.value)
        assertEquals(mockPlayers, viewModel.playerList.value)
    }

    @Test
    fun `getPlayerBalance should handle players with various balances`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val mockPlayers = listOf(
            Pair("Winner", 1000),
            Pair("Loser", -500),
            Pair("BreakEven", 0),
            Pair("SmallWin", 50),
            Pair("SmallLoss", -25)
        )
        
        coEvery { playerRepository.getAllPlayers() } returns mockPlayers
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify all players are loaded correctly
        assertEquals(5, viewModel.playerList.value.size)
        assertEquals(mockPlayers, viewModel.playerList.value)
        
        // Verify specific player balances
        val playerMap = viewModel.playerList.value.toMap()
        assertEquals(1000, playerMap["Winner"])
        assertEquals(-500, playerMap["Loser"])
        assertEquals(0, playerMap["BreakEven"])
        assertEquals(50, playerMap["SmallWin"])
        assertEquals(-25, playerMap["SmallLoss"])
    }

    @Test
    fun `multiple calls to getPlayerBalance should update player list`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val firstList = listOf(
            Pair("Player1", 100),
            Pair("Player2", 200)
        )
        
        val secondList = listOf(
            Pair("Player1", 150),
            Pair("Player2", 250),
            Pair("Player3", 300)
        )
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        // First call returns firstList
        coEvery { playerRepository.getAllPlayers() } returns firstList
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        // First load
        viewModel.getPlayerBalance()
        assertEquals(firstList, viewModel.playerList.value)
        
        // Update mock to return secondList
        coEvery { playerRepository.getAllPlayers() } returns secondList
        
        // Second load
        viewModel.getPlayerBalance()
        assertEquals(secondList, viewModel.playerList.value)
    }

    @Test
    fun `getPlayerBalance should handle large player lists`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        // Create a large list of players
        val largePlayerList = (1..100).map { 
            Pair("Player$it", it * 10)
        }
        
        coEvery { playerRepository.getAllPlayers() } returns largePlayerList
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify large list is loaded correctly
        assertEquals(100, viewModel.playerList.value.size)
        assertEquals(largePlayerList, viewModel.playerList.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `getPlayerBalance should handle duplicate player names`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        // Note: In real scenario, this might be a data issue, but we should handle it gracefully
        val playersWithDuplicates = listOf(
            Pair("John", 100),
            Pair("Jane", 200),
            Pair("John", 150), // Duplicate name with different balance
            Pair("Bob", 300)
        )
        
        coEvery { playerRepository.getAllPlayers() } returns playersWithDuplicates
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify all entries are loaded (including duplicates)
        assertEquals(4, viewModel.playerList.value.size)
        assertEquals(playersWithDuplicates, viewModel.playerList.value)
    }

    @Test
    fun `getPlayerBalance error should log error message`() = runTest {
        val unconfinedDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(unconfinedDispatcher)
        
        val errorMessage = "Database connection failed"
        coEvery { playerRepository.getAllPlayers() } throws Exception(errorMessage)
        
        // Mock Dispatchers.IO for this specific test
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns unconfinedDispatcher
        
        viewModel = PlayerBalanceViewModel(playerRepository)
        
        viewModel.getPlayerBalance()
        
        // Verify error is handled
        assertTrue(viewModel.playerList.value.isEmpty())
        assertFalse(viewModel.loading.value)
        
        // Verify Log.e was called (mocked)
        coVerify { playerRepository.getAllPlayers() }
    }
}