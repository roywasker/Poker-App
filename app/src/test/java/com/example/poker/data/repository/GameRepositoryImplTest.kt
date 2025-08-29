package com.example.poker.data.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GameRepositoryImplTest {

    private lateinit var repository: GameRepositoryImpl
    private lateinit var playersRef: DatabaseReference
    private lateinit var dateListRef: DatabaseReference
    private lateinit var mockPlayersSnapshot: DataSnapshot
    private lateinit var mockDateSnapshot: DataSnapshot

    @Before
    fun setup() {
        playersRef = mockk(relaxed = true)
        dateListRef = mockk(relaxed = true)
        mockPlayersSnapshot = mockk(relaxed = true)
        mockDateSnapshot = mockk(relaxed = true)
        repository = GameRepositoryImpl(playersRef, dateListRef)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        
        // Mock kotlinx.coroutines.tasks.await extension
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // Tests for getAllDates
    @Test
    fun `getAllDates should return list of dates with keys`() = runTest {
        val date1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { date1Snapshot.child("date").getValue(String::class.java) } returns "2024-01-10"
        every { date1Snapshot.key } returns "key1"
        
        every { date2Snapshot.child("date").getValue(String::class.java) } returns "2024-01-11"
        every { date2Snapshot.key } returns "key2"
        
        every { date3Snapshot.child("date").getValue(String::class.java) } returns "2024-01-12"
        every { date3Snapshot.key } returns "key3"
        
        every { mockDateSnapshot.children } returns listOf(date1Snapshot, date2Snapshot, date3Snapshot)
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getAllDates()
        
        assertEquals(3, result.size)
        assertEquals("2024-01-10" to "key1", result[0])
        assertEquals("2024-01-11" to "key2", result[1])
        assertEquals("2024-01-12" to "key3", result[2])
    }

    @Test
    fun `getAllDates should skip entries with null dates`() = runTest {
        val date1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { date1Snapshot.child("date").getValue(String::class.java) } returns "2024-01-10"
        every { date1Snapshot.key } returns "key1"
        
        every { date2Snapshot.child("date").getValue(String::class.java) } returns null
        every { date2Snapshot.key } returns "key2"
        
        every { mockDateSnapshot.children } returns listOf(date1Snapshot, date2Snapshot)
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getAllDates()
        
        assertEquals(1, result.size)
        assertEquals("2024-01-10" to "key1", result[0])
    }

    @Test
    fun `getAllDates should return empty list when no dates exist`() = runTest {
        every { mockDateSnapshot.children } returns emptyList<DataSnapshot>()
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getAllDates()
        
        assertTrue(result.isEmpty())
    }

    // Tests for getPlayerBalanceByDate
    @Test
    fun `getPlayerBalanceByDate should return sorted player balances`() = runTest {
        val dateKey = "dateKey123"
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { dateListRef.child(dateKey).child("playerBalance") } returns balanceRef
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 300
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns -50
        
        every { mockDateSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { balanceRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerBalanceByDate(dateKey)
        
        assertEquals(3, result.size)
        // Should be sorted by balance descending
        assertEquals("Player2" to 300, result[0])
        assertEquals("Player1" to 100, result[1])
        assertEquals("Player3" to -50, result[2])
    }

    @Test
    fun `getPlayerBalanceByDate should skip entries with null values`() = runTest {
        val dateKey = "dateKey123"
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { dateListRef.child(dateKey).child("playerBalance") } returns balanceRef
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns null
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 200
        
        every { mockDateSnapshot.children } returns listOf(player1Snapshot, player2Snapshot)
        every { balanceRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerBalanceByDate(dateKey)
        
        assertEquals(1, result.size)
        assertEquals("Player1" to 100, result[0])
    }

    // Tests for getPlayersForStartGame
    @Test
    fun `getPlayersForStartGame should return list of player names`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        
        every { mockPlayersSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        val result = repository.getPlayersForStartGame()
        
        assertEquals(listOf("Player1", "Player2", "Player3"), result)
    }

    @Test
    fun `getPlayersForStartGame should skip null names`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player2Snapshot.child("name").getValue(String::class.java) } returns null
        
        every { mockPlayersSnapshot.children } returns listOf(player1Snapshot, player2Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        val result = repository.getPlayersForStartGame()
        
        assertEquals(listOf("Player1"), result)
    }

    // Tests for updatePlayerBalance
    @Test
    fun `updatePlayerBalance should update balances and add history successfully`() = runTest {
        val balanceAfterGame = arrayOf(50, -50, 0)
        val nameOfPlayer = arrayOf("Player1", "Player2", "Player3")
        
        // Mock existing players
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player1Ref = mockk<DatabaseReference>(relaxed = true)
        val player2Ref = mockk<DatabaseReference>(relaxed = true)
        val player3Ref = mockk<DatabaseReference>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        every { player1Snapshot.ref } returns player1Ref
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 200
        every { player2Snapshot.ref } returns player2Ref
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 150
        every { player3Snapshot.ref } returns player3Ref
        
        every { mockPlayersSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        // Mock balance updates
        every { player1Ref.child("balance").setValue(150) } returns Tasks.forResult(null)
        every { player2Ref.child("balance").setValue(150) } returns Tasks.forResult(null)
        every { player3Ref.child("balance").setValue(150) } returns Tasks.forResult(null)
        
        // Mock date history creation
        val dateId = "dateId123"
        val newDateRef = mockk<DatabaseReference>(relaxed = true)
        val playerBalanceRef = mockk<DatabaseReference>(relaxed = true)
        
        every { dateListRef.push() } returns newDateRef
        every { newDateRef.key } returns dateId
        every { dateListRef.child(dateId) } returns newDateRef
        every { newDateRef.setValue(any()) } returns Tasks.forResult(null)
        every { newDateRef.child("playerBalance") } returns playerBalanceRef
        
        val playerIdRef = mockk<DatabaseReference>(relaxed = true)
        every { playerBalanceRef.push() } returns playerIdRef
        every { playerIdRef.key } returns "playerId"
        every { playerBalanceRef.child(any()) } returns playerIdRef
        every { playerIdRef.setValue(any()) } returns Tasks.forResult(null)
        
        val result = repository.updatePlayerBalance(balanceAfterGame, nameOfPlayer)
        
        assertTrue(result)
        
        // Verify balances were updated
        verify { player1Ref.child("balance").setValue(150) } // 100 + 50
        verify { player2Ref.child("balance").setValue(150) } // 200 - 50
        verify { player3Ref.child("balance").setValue(150) } // 150 + 0
        
        // Verify date was saved (using current date)
        val dateSlot = slot<Map<String, String>>()
        verify { newDateRef.setValue(capture(dateSlot)) }
        assertNotNull(dateSlot.captured["date"])
        assertTrue(dateSlot.captured["date"]!!.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `updatePlayerBalance should skip empty player names`() = runTest {
        val balanceAfterGame = arrayOf(50, -50, 0, 0)
        val nameOfPlayer = arrayOf("Player1", "", "Player3", "")
        
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player1Ref = mockk<DatabaseReference>(relaxed = true)
        val player3Ref = mockk<DatabaseReference>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        every { player1Snapshot.ref } returns player1Ref
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 150
        every { player3Snapshot.ref } returns player3Ref
        
        every { mockPlayersSnapshot.children } returns listOf(player1Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        every { player1Ref.child("balance").setValue(150) } returns Tasks.forResult(null)
        every { player3Ref.child("balance").setValue(150) } returns Tasks.forResult(null)
        
        // Mock date history
        val dateId = "dateId123"
        val newDateRef = mockk<DatabaseReference>(relaxed = true)
        val playerBalanceRef = mockk<DatabaseReference>(relaxed = true)
        val playerIdRef = mockk<DatabaseReference>(relaxed = true)
        
        every { dateListRef.push() } returns newDateRef
        every { newDateRef.key } returns dateId
        every { dateListRef.child(dateId) } returns newDateRef
        every { newDateRef.setValue(any()) } returns Tasks.forResult(null)
        every { newDateRef.child("playerBalance") } returns playerBalanceRef
        every { playerBalanceRef.push() } returns playerIdRef
        every { playerIdRef.key } returns "playerId"
        every { playerBalanceRef.child(any()) } returns playerIdRef
        every { playerIdRef.setValue(any()) } returns Tasks.forResult(null)
        
        val result = repository.updatePlayerBalance(balanceAfterGame, nameOfPlayer)
        
        assertTrue(result)
    }

    @Test
    fun `updatePlayerBalance should return false on exception`() = runTest {
        val balanceAfterGame = arrayOf(50, -50)
        val nameOfPlayer = arrayOf("Player1", "Player2")
        
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.updatePlayerBalance(balanceAfterGame, nameOfPlayer)
        
        assertFalse(result)
        verify { Log.e("ContentValues", "updatePlayerBalance: java.lang.Exception: Database error") }
    }

    // Tests for getPlayerHistoricalData
    @Test
    fun `getPlayerHistoricalData should return sorted history for player`() = runTest {
        val playerName = "TestPlayer"
        
        val date1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { date1Snapshot.child("date").getValue(String::class.java) } returns "2024-01-10"
        every { date2Snapshot.child("date").getValue(String::class.java) } returns "2024-01-05"
        
        val player1Balance1 = mockk<DataSnapshot>(relaxed = true)
        val player2Balance1 = mockk<DataSnapshot>(relaxed = true)
        val player1Balance2 = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Balance1.child("name").getValue(String::class.java) } returns playerName
        every { player1Balance1.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Balance1.child("name").getValue(String::class.java) } returns "OtherPlayer"
        every { player2Balance1.child("balance").getValue(Int::class.java) } returns 50
        
        every { player1Balance2.child("name").getValue(String::class.java) } returns playerName
        every { player1Balance2.child("balance").getValue(Int::class.java) } returns -30
        
        every { date1Snapshot.child("playerBalance").children } returns listOf(player1Balance1, player2Balance1)
        every { date2Snapshot.child("playerBalance").children } returns listOf(player1Balance2)
        
        every { mockDateSnapshot.children } returns listOf(date1Snapshot, date2Snapshot)
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerHistoricalData(playerName)
        
        assertEquals(2, result.size)
        // Should be sorted by date
        assertEquals("2024-01-05" to -30, result[0])
        assertEquals("2024-01-10" to 100, result[1])
    }

    @Test
    fun `getPlayerHistoricalData should return empty list for non-existent player`() = runTest {
        val playerName = "NonExistentPlayer"
        
        val dateSnapshot = mockk<DataSnapshot>(relaxed = true)
        every { dateSnapshot.child("date").getValue(String::class.java) } returns "2024-01-10"
        
        val playerBalance = mockk<DataSnapshot>(relaxed = true)
        every { playerBalance.child("name").getValue(String::class.java) } returns "OtherPlayer"
        every { playerBalance.child("balance").getValue(Int::class.java) } returns 100
        
        every { dateSnapshot.child("playerBalance").children } returns listOf(playerBalance)
        every { mockDateSnapshot.children } returns listOf(dateSnapshot)
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerHistoricalData(playerName)
        
        assertTrue(result.isEmpty())
    }

    // Tests for getPlayerStatistics
    @Test
    fun `getPlayerStatistics should return statistics for existing player`() = runTest {
        val playerName = "TestPlayer"
        
        // Mock current balance
        val playerSnapshot = mockk<DataSnapshot>(relaxed = true)
        every { playerSnapshot.child("name").getValue(String::class.java) } returns playerName
        every { playerSnapshot.child("balance").getValue(Int::class.java) } returns 500
        every { mockPlayersSnapshot.children } returns listOf(playerSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        // Mock historical data
        val date1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val date3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { date1Snapshot.child("date").getValue(String::class.java) } returns "2024-01-10"
        every { date2Snapshot.child("date").getValue(String::class.java) } returns "2024-01-11"
        every { date3Snapshot.child("date").getValue(String::class.java) } returns "2024-01-12"
        
        val player1Balance1 = mockk<DataSnapshot>(relaxed = true)
        val player1Balance2 = mockk<DataSnapshot>(relaxed = true)
        val player1Balance3 = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Balance1.child("name").getValue(String::class.java) } returns playerName
        every { player1Balance1.child("balance").getValue(Int::class.java) } returns 100
        
        every { player1Balance2.child("name").getValue(String::class.java) } returns playerName
        every { player1Balance2.child("balance").getValue(Int::class.java) } returns -50
        
        every { player1Balance3.child("name").getValue(String::class.java) } returns playerName
        every { player1Balance3.child("balance").getValue(Int::class.java) } returns 75
        
        every { date1Snapshot.child("playerBalance").children } returns listOf(player1Balance1)
        every { date2Snapshot.child("playerBalance").children } returns listOf(player1Balance2)
        every { date3Snapshot.child("playerBalance").children } returns listOf(player1Balance3)
        
        every { mockDateSnapshot.children } returns listOf(date1Snapshot, date2Snapshot, date3Snapshot)
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerStatistics(playerName)
        
        assertNotNull(result)
        assertEquals(playerName, result!!.playerName)
        assertEquals(3, result.totalGames)
        assertEquals(2, result.gamesWon) // 100 and 75
        assertEquals(1, result.gamesLost) // -50
        assertEquals(125, result.totalWinnings) // 100 - 50 + 75
        assertEquals(500, result.currentBalance)
        assertEquals(100, result.bestGame)
        assertEquals(-50, result.worstGame)
    }

    @Test
    fun `getPlayerStatistics should return null on exception`() = runTest {
        val playerName = "TestPlayer"
        
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.getPlayerStatistics(playerName)
        
        assertNull(result)
        verify { Log.e("ContentValues", "getPlayerStatistics: java.lang.Exception: Database error") }
    }

    @Test
    fun `getPlayerStatistics should handle player with no history`() = runTest {
        val playerName = "NewPlayer"
        
        // Mock current balance
        val playerSnapshot = mockk<DataSnapshot>(relaxed = true)
        every { playerSnapshot.child("name").getValue(String::class.java) } returns playerName
        every { playerSnapshot.child("balance").getValue(Int::class.java) } returns 0
        every { mockPlayersSnapshot.children } returns listOf(playerSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        // Mock empty historical data
        every { mockDateSnapshot.children } returns emptyList<DataSnapshot>()
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getPlayerStatistics(playerName)
        
        assertNotNull(result)
        assertEquals(playerName, result!!.playerName)
        assertEquals(0, result.totalGames)
        assertEquals(0, result.gamesWon)
        assertEquals(0, result.gamesLost)
        assertEquals(0, result.currentBalance)
        assertEquals(0.0, result.averageWinLoss, 0.01)
    }

    // Tests for getAllPlayersStatistics
    @Test
    fun `getAllPlayersStatistics should return sorted list of all player statistics`() = runTest {
        // Mock player names
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 200
        
        every { mockPlayersSnapshot.children } returnsMany listOf(
            listOf(player1Snapshot, player2Snapshot), // For getPlayersForStartGame
            listOf(player1Snapshot), // For getPlayerStatistics("Player1")
            listOf(player2Snapshot)  // For getPlayerStatistics("Player2")
        )
        every { playersRef.get() } returns Tasks.forResult(mockPlayersSnapshot)
        
        // Mock empty historical data for simplicity
        every { mockDateSnapshot.children } returns emptyList<DataSnapshot>()
        every { dateListRef.get() } returns Tasks.forResult(mockDateSnapshot)
        
        val result = repository.getAllPlayersStatistics()
        
        assertEquals(2, result.size)
        // Should be sorted by current balance descending
        assertEquals("Player2", result[0].playerName)
        assertEquals(200, result[0].currentBalance)
        assertEquals("Player1", result[1].playerName)
        assertEquals(100, result[1].currentBalance)
    }

    @Test
    fun `getAllPlayersStatistics should return empty list on exception`() = runTest {
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.getAllPlayersStatistics()
        
        assertTrue(result.isEmpty())
        verify { Log.e("ContentValues", "getAllPlayersStatistics: java.lang.Exception: Database error") }
    }
}