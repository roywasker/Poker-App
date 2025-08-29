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
class StartGameViewModelTest {

    private lateinit var viewModel: StartGameViewModel
    private lateinit var sessionRepository: PlayerSessionRepository
    private lateinit var gameRepository: GameRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        gameRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
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

    // Tests for initialization
    @Test
    fun `init should check for active session on startup`() = runTest {
        coEvery { sessionRepository.hasActiveSession() } returns false
        
        viewModel = createViewModel()
        
        coVerify { sessionRepository.hasActiveSession() }
        assertFalse(viewModel.hasActiveSession.value)
    }

    @Test
    fun `init should load saved session if exists`() = runTest {
        val mockSessionData = SessionData(
            numOfRows = 3,
            playerNames = Array(9) { mutableStateOf(if (it < 3) "Player$it" else "") },
            buyAmounts = Array(9) { mutableStateOf(if (it < 3) "100" else "") },
            returnAmounts = Array(9) { mutableStateOf(if (it < 3) "150" else "") }
        )
        
        coEvery { sessionRepository.hasActiveSession() } returns true
        coEvery { sessionRepository.loadSession() } returns mockSessionData
        
        viewModel = createViewModel()
        
        coVerify { sessionRepository.loadSession() }
        assertTrue(viewModel.hasActiveSession.value)
        assertEquals(3, viewModel.numOfRows.intValue)
        assertEquals("Player0", viewModel.nameOfPlayerArray[0].value)
        assertEquals("100", viewModel.buyMoneyArray[0].value)
        assertEquals("150", viewModel.returnMoneyArray[0].value)
    }

    // Tests for player list management
    @Test
    fun `addPlayerToList should add player to chosen list and remove from available`() = runTest {
        viewModel = createViewModel()
        viewModel.playerList.value = listOf("Player1", "Player2", "Player3")
        
        viewModel.addPlayerToList("Player2")
        
        assertEquals(listOf("Player1", "Player3"), viewModel.playerList.value)
        // Note: playerListChosen is private, we can't directly verify it
        coVerify { sessionRepository.saveSession(any(), any(), any(), any()) }
    }

    @Test
    fun `removePlayerFromList should remove player from chosen and add to available`() = runTest {
        viewModel = createViewModel()
        viewModel.playerList.value = listOf("Player1", "Player3")
        
        viewModel.removePlayerFromList("Player2")
        
        assertEquals(listOf("Player1", "Player3", "Player2"), viewModel.playerList.value)
        coVerify { sessionRepository.saveSession(any(), any(), any(), any()) }
    }

    // Tests for removeRow
    @Test
    fun `removeRow should reset row data and add player back to list`() = runTest {
        viewModel = createViewModel()
        viewModel.nameOfPlayerArray[0].value = "TestPlayer"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.playerList.value = listOf("Player1", "Player2")
        
        viewModel.removeRow(0)
        
        assertEquals("", viewModel.nameOfPlayerArray[0].value)
        assertEquals("", viewModel.buyMoneyArray[0].value)
        assertEquals("", viewModel.returnMoneyArray[0].value)
        assertEquals(listOf("Player1", "Player2", "TestPlayer"), viewModel.playerList.value)
        coVerify { sessionRepository.saveSession(any(), any(), any(), any()) }
    }

    @Test
    fun `removeRow should not add empty player name to list`() = runTest {
        viewModel = createViewModel()
        viewModel.nameOfPlayerArray[1].value = ""
        viewModel.playerList.value = listOf("Player1", "Player2")
        
        viewModel.removeRow(1)
        
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
    }

    @Test
    fun `removeRow should not add duplicate player to list`() = runTest {
        viewModel = createViewModel()
        viewModel.nameOfPlayerArray[2].value = "Player1"
        viewModel.playerList.value = listOf("Player1", "Player2")
        
        viewModel.removeRow(2)
        
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
    }

    // Tests for getPlayerListToStartGame
    @Test
    fun `getPlayerListToStartGame should load players and set loading state`() = runTest {
        val mockPlayers = listOf("Player1", "Player2", "Player3")
        coEvery { gameRepository.getPlayersForStartGame() } returns mockPlayers
        
        viewModel = createViewModel()
        viewModel.getPlayerListToStartGame()
        
        assertEquals(mockPlayers, viewModel.playerList.value)
        assertFalse(viewModel.loading.value)
    }

    @Test
    fun `getPlayerListToStartGame should handle exception`() = runTest {
        coEvery { gameRepository.getPlayersForStartGame() } throws Exception("Network error")
        
        viewModel = createViewModel()
        viewModel.getPlayerListToStartGame()
        
        assertFalse(viewModel.loading.value)
    }

    // Tests for clearMessageDialog
    @Test
    fun `clearMessageDialog should set messageDialog to null`() = runTest {
        viewModel = createViewModel()
        // Simulate setting a message (we'll need to trigger an error condition)
        viewModel.nameOfPlayerArray[0].value = ""
        viewModel.finishGameButton()
        
        assertNotNull(viewModel.messageDialog.value)
        
        viewModel.clearMessageDialog()
        
        assertNull(viewModel.messageDialog.value)
    }

    // Tests for clearSavedSession
    @Test
    fun `clearSavedSession should clear session and reset data`() = runTest {
        viewModel = createViewModel()
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel.clearSavedSession()
        
        coVerify { sessionRepository.clearSession() }
        assertFalse(viewModel.hasActiveSession.value)
        assertEquals(4, viewModel.numOfRows.intValue)
    }

    // Tests for onDataChanged
    @Test
    fun `onDataChanged should save current session`() = runTest {
        viewModel = createViewModel()
        
        viewModel.onDataChanged()
        
        coVerify { sessionRepository.saveSession(any(), any(), any(), any()) }
        assertTrue(viewModel.hasActiveSession.value)
    }

    // Tests for finishGameButton - validation scenarios
    @Test
    fun `finishGameButton should return false when fields are empty`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = ""
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("Please fill all the field.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when buy money is empty`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.buyMoneyArray[0].value = ""
        viewModel.returnMoneyArray[0].value = "100"
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("Please fill all the field.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when return money is empty`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.returnMoneyArray[0].value = ""
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("Please fill all the field.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when buy money contains non-digits`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 1
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.buyMoneyArray[0].value = "100a"
        viewModel.returnMoneyArray[0].value = "100"
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("Please enter numbers only.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when return money contains non-digits`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 1
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.returnMoneyArray[0].value = "abc"
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("Please enter numbers only.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when there is extra money`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "50"
        viewModel.returnMoneyArray[1].value = "100"  // Total: -50 + 0 = -50 (negative sum means extra)
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("You have a extra of 50 shekels.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should return false when there is deficit money`() = runTest {
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "100"  // Total: 50 + 0 = 50 (positive sum means deficit)
        
        val result = viewModel.finishGameButton()
        
        assertFalse(result)
        assertEquals("You have a deficit of 50 shekels.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should process valid balanced game`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "50"  // Player1: +50, Player2: -50 = balanced
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        verify { TransferLogRepository.addLogEntry("Player2 transfer 50 to Player1") }
        // Wait for async database update
        Thread.sleep(100)
        // The ViewModel passes full arrays with 9 elements
        coVerify { 
            gameRepository.updatePlayerBalance(
                arrayOf(50, -50), 
                match { it[0] == "Player1" && it[1] == "Player2" }
            ) 
        }
    }

    @Test
    fun `finishGameButton should handle database update failure`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns false
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "50"
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result) // finishGameButton returns true, error is handled in updateBalanceInDataBase
        // Wait for async operation
        Thread.sleep(100)
        assertEquals("Failed to update game data. Please try again.", viewModel.messageDialog.value)
    }

    @Test
    fun `finishGameButton should handle database exception`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } throws Exception("Database error")
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "50"
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        // Wait for async operation
        Thread.sleep(100)
        assertEquals("Failed to update game data. Please try again.", viewModel.messageDialog.value)
    }

    // Tests for complex transfer scenarios
    @Test
    fun `finishGameButton should handle multiple winners and losers`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 4
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.nameOfPlayerArray[2].value = "Player3"
        viewModel.nameOfPlayerArray[3].value = "Player4"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.buyMoneyArray[2].value = "100"
        viewModel.buyMoneyArray[3].value = "100"
        viewModel.returnMoneyArray[0].value = "200"  // +100
        viewModel.returnMoneyArray[1].value = "150"  // +50
        viewModel.returnMoneyArray[2].value = "50"   // -50
        viewModel.returnMoneyArray[3].value = "0"    // -100
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        // Verify transfers are calculated correctly (Player4 pays 100 to Player1, Player3 pays 50 to Player2)
        verify { TransferLogRepository.addLogEntry("Player4 transfer 100 to Player1") }
        verify { TransferLogRepository.addLogEntry("Player3 transfer 50 to Player2") }
    }

    @Test
    fun `finishGameButton should handle partial transfers`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 3
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.nameOfPlayerArray[2].value = "Player3"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.buyMoneyArray[2].value = "100"
        viewModel.returnMoneyArray[0].value = "250"  // +150
        viewModel.returnMoneyArray[1].value = "50"   // -50
        viewModel.returnMoneyArray[2].value = "0"    // -100
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        // Player3 transfers 100 to Player1, Player2 transfers 50 to Player1
        verify { TransferLogRepository.addLogEntry("Player3 transfer 100 to Player1") }
        verify { TransferLogRepository.addLogEntry("Player2 transfer 50 to Player1") }
    }

    @Test
    fun `finishGameButton with zero balance player should be ignored in transfers`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 3
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.nameOfPlayerArray[2].value = "Player3"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.buyMoneyArray[2].value = "100"
        viewModel.returnMoneyArray[0].value = "150"  // +50
        viewModel.returnMoneyArray[1].value = "100"  // 0
        viewModel.returnMoneyArray[2].value = "50"   // -50
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        verify { TransferLogRepository.addLogEntry("Player3 transfer 50 to Player1") }
        // Player2 with 0 balance should not appear in any transfer
    }

    // Edge cases
    @Test
    fun `finishGameButton should handle maximum players`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 9  // Maximum supported
        for (i in 0 until 9) {
            viewModel.nameOfPlayerArray[i].value = "Player$i"
            viewModel.buyMoneyArray[i].value = "100"
            viewModel.returnMoneyArray[i].value = if (i == 0) "900" else "0"  // Player0 wins all
        }
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        // All players should transfer to Player0
        for (i in 1 until 9) {
            verify { TransferLogRepository.addLogEntry("Player$i transfer 100 to Player0") }
        }
    }

    @Test
    fun `finishGameButton should handle single player game`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 1
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.returnMoneyArray[0].value = "100"  // No change
        
        val result = viewModel.finishGameButton()
        
        assertTrue(result)
        verify { TransferLogRepository.cleanTransferLog() }
        // No transfers should be added for a single player with no change
        verify(exactly = 0) { TransferLogRepository.addLogEntry(any()) }
    }

    @Test
    fun `session should be cleared after successful game completion`() = runTest {
        coEvery { gameRepository.updatePlayerBalance(any(), any()) } returns true
        coEvery { gameRepository.getPlayersForStartGame() } returns emptyList()
        
        viewModel = createViewModel()
        viewModel.numOfRows.intValue = 2
        viewModel.nameOfPlayerArray[0].value = "Player1"
        viewModel.nameOfPlayerArray[1].value = "Player2"
        viewModel.buyMoneyArray[0].value = "100"
        viewModel.buyMoneyArray[1].value = "100"
        viewModel.returnMoneyArray[0].value = "150"
        viewModel.returnMoneyArray[1].value = "50"
        
        viewModel.finishGameButton()
        
        // Wait for async operation
        Thread.sleep(100)
        
        coVerify { sessionRepository.clearSession() }
        assertFalse(viewModel.hasActiveSession.value)
    }

    @Test
    fun `loading state should be managed correctly during player list fetch`() = runTest {
        coEvery { gameRepository.getPlayersForStartGame() } returns listOf("Player1", "Player2")
        
        viewModel = createViewModel()
        
        // Call the method - loading will be set and unset synchronously due to UnconfinedTestDispatcher
        viewModel.getPlayerListToStartGame()
        
        // With UnconfinedTestDispatcher, the coroutine executes immediately
        // So loading is already false after the method returns
        assertFalse(viewModel.loading.value)
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
    }
}