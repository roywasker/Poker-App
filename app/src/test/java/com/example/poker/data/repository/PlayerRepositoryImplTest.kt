package com.example.poker.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerRepositoryImplTest {

    private lateinit var repository: PlayerRepositoryImpl
    private lateinit var playersRef: DatabaseReference
    private lateinit var mockSnapshot: DataSnapshot

    @Before
    fun setup() {
        playersRef = mockk(relaxed = true)
        mockSnapshot = mockk(relaxed = true)
        repository = PlayerRepositoryImpl(playersRef)
        
        // Mock kotlinx.coroutines.tasks.await extension
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `getAllPlayers should return sorted list of players by balance descending`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 500
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 250
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(3, result.size)
        // Should be sorted by balance descending
        assertEquals("Player2" to 500, result[0])
        assertEquals("Player3" to 250, result[1])
        assertEquals("Player1" to 100, result[2])
    }

    @Test
    fun `getAllPlayers should skip players with null name`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns null
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 500
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 250
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(2, result.size)
        assertEquals("Player3" to 250, result[0])
        assertEquals("Player1" to 100, result[1])
    }

    @Test
    fun `getAllPlayers should skip players with null balance`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns null
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 250
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(2, result.size)
        assertEquals("Player3" to 250, result[0])
        assertEquals("Player1" to 100, result[1])
    }

    @Test
    fun `getAllPlayers should return empty list when no players exist`() = runTest {
        every { mockSnapshot.children } returns emptyList<DataSnapshot>()
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllPlayers should handle negative balances`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns -50
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns -200
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(3, result.size)
        assertEquals("Player2" to 100, result[0])
        assertEquals("Player1" to -50, result[1])
        assertEquals("Player3" to -200, result[2])
    }

    @Test
    fun `getAllPlayers should handle zero balances`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 0
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 0
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(3, result.size)
        assertEquals("Player2" to 100, result[0])
        assertEquals("Player1" to 0, result[1])
        assertEquals("Player3" to 0, result[2])
    }

    @Test
    fun `getAllPlayers should handle players with same balance`() = runTest {
        val player1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val player3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { player1Snapshot.child("name").getValue(String::class.java) } returns "Player1"
        every { player1Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player2Snapshot.child("name").getValue(String::class.java) } returns "Player2"
        every { player2Snapshot.child("balance").getValue(Int::class.java) } returns 100
        
        every { player3Snapshot.child("name").getValue(String::class.java) } returns "Player3"
        every { player3Snapshot.child("balance").getValue(Int::class.java) } returns 50
        
        every { mockSnapshot.children } returns listOf(player1Snapshot, player2Snapshot, player3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(3, result.size)
        // First two should have balance 100, last one 50
        assertEquals(100, result[0].second)
        assertEquals(100, result[1].second)
        assertEquals(50, result[2].second)
        assertTrue(result[0].first in listOf("Player1", "Player2"))
        assertTrue(result[1].first in listOf("Player1", "Player2"))
        assertEquals("Player3", result[2].first)
    }

    @Test
    fun `getAllPlayers should handle single player`() = runTest {
        val playerSnapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { playerSnapshot.child("name").getValue(String::class.java) } returns "OnlyPlayer"
        every { playerSnapshot.child("balance").getValue(Int::class.java) } returns 1000
        
        every { mockSnapshot.children } returns listOf(playerSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getAllPlayers()
        
        assertEquals(1, result.size)
        assertEquals("OnlyPlayer" to 1000, result[0])
    }
}