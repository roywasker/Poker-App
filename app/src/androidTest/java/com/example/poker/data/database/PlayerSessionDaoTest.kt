package com.example.poker.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlayerSessionDaoTest {

    private lateinit var database: PokerDatabase
    private lateinit var dao: PlayerSessionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PokerDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.playerSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saveSession should insert new session`() = runTest {
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "test-session-123",
            numOfRows = 3,
            playerNames = "[\"Player1\",\"Player2\",\"Player3\"]",
            buyAmounts = "[\"100\",\"200\",\"150\"]",
            returnAmounts = "[\"150\",\"100\",\"200\"]"
        )
        
        dao.saveSession(session)
        
        val retrieved = dao.getCurrentSessionOnce()
        assertNotNull(retrieved)
        assertEquals("test-session-123", retrieved?.sessionId)
        assertEquals(3, retrieved?.numOfRows)
        assertEquals("[\"Player1\",\"Player2\",\"Player3\"]", retrieved?.playerNames)
    }

    @Test
    fun `saveSession should replace existing session with same id`() = runTest {
        val session1 = PlayerSessionEntity(
            id = 0,
            sessionId = "session-1",
            numOfRows = 2,
            playerNames = "[\"Player1\",\"Player2\"]",
            buyAmounts = "[\"100\",\"200\"]",
            returnAmounts = "[\"150\",\"150\"]"
        )
        
        val session2 = PlayerSessionEntity(
            id = 0,
            sessionId = "session-2",
            numOfRows = 4,
            playerNames = "[\"PlayerA\",\"PlayerB\",\"PlayerC\",\"PlayerD\"]",
            buyAmounts = "[\"50\",\"100\",\"150\",\"200\"]",
            returnAmounts = "[\"100\",\"100\",\"100\",\"100\"]"
        )
        
        dao.saveSession(session1)
        dao.saveSession(session2)
        
        val retrieved = dao.getCurrentSessionOnce()
        assertNotNull(retrieved)
        assertEquals("session-2", retrieved?.sessionId)
        assertEquals(4, retrieved?.numOfRows)
    }

    @Test
    fun `getCurrentSessionOnce should return null when no session exists`() = runTest {
        val session = dao.getCurrentSessionOnce()
        assertNull(session)
    }

    @Test
    fun `clearSession should remove existing session`() = runTest {
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "test-session",
            numOfRows = 2,
            playerNames = "[\"Player1\",\"Player2\"]",
            buyAmounts = "[\"100\",\"200\"]",
            returnAmounts = "[\"150\",\"150\"]"
        )
        
        dao.saveSession(session)
        assertNotNull(dao.getCurrentSessionOnce())
        
        dao.clearSession()
        
        assertNull(dao.getCurrentSessionOnce())
    }

    @Test
    fun `clearSession should do nothing when no session exists`() = runTest {
        assertNull(dao.getCurrentSessionOnce())
        
        dao.clearSession()
        
        assertNull(dao.getCurrentSessionOnce())
    }

    @Test
    fun `hasActiveSession should return true when session exists`() = runTest {
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "test-session",
            numOfRows = 1,
            playerNames = "[\"Player1\"]",
            buyAmounts = "[\"100\"]",
            returnAmounts = "[\"100\"]"
        )
        
        dao.saveSession(session)
        
        assertTrue(dao.hasActiveSession())
    }

    @Test
    fun `hasActiveSession should return false when no session exists`() = runTest {
        assertFalse(dao.hasActiveSession())
    }

    @Test
    fun `hasActiveSession should return false after clearing session`() = runTest {
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "test-session",
            numOfRows = 1,
            playerNames = "[\"Player1\"]",
            buyAmounts = "[\"100\"]",
            returnAmounts = "[\"100\"]"
        )
        
        dao.saveSession(session)
        assertTrue(dao.hasActiveSession())
        
        dao.clearSession()
        assertFalse(dao.hasActiveSession())
    }

    @Test
    fun `saveSession should handle empty JSON arrays`() = runTest {
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "empty-session",
            numOfRows = 0,
            playerNames = "[]",
            buyAmounts = "[]",
            returnAmounts = "[]"
        )
        
        dao.saveSession(session)
        
        val retrieved = dao.getCurrentSessionOnce()
        assertNotNull(retrieved)
        assertEquals("[]", retrieved?.playerNames)
        assertEquals("[]", retrieved?.buyAmounts)
        assertEquals("[]", retrieved?.returnAmounts)
    }

    @Test
    fun `saveSession should handle large player counts`() = runTest {
        val playerNames = (1..9).map { "\"Player$it\"" }.joinToString(",", "[", "]")
        val buyAmounts = (1..9).map { "\"${it * 100}\"" }.joinToString(",", "[", "]")
        val returnAmounts = (1..9).map { "\"${it * 50}\"" }.joinToString(",", "[", "]")
        
        val session = PlayerSessionEntity(
            id = 0,
            sessionId = "large-session",
            numOfRows = 9,
            playerNames = playerNames,
            buyAmounts = buyAmounts,
            returnAmounts = returnAmounts
        )
        
        dao.saveSession(session)
        
        val retrieved = dao.getCurrentSessionOnce()
        assertNotNull(retrieved)
        assertEquals(9, retrieved?.numOfRows)
        assertEquals(playerNames, retrieved?.playerNames)
    }

    @Test
    fun `multiple operations should maintain data integrity`() = runTest {
        // Start with no session
        assertFalse(dao.hasActiveSession())
        
        // Add first session
        val session1 = PlayerSessionEntity(
            id = 0,
            sessionId = "session-1",
            numOfRows = 2,
            playerNames = "[\"Player1\",\"Player2\"]",
            buyAmounts = "[\"100\",\"200\"]",
            returnAmounts = "[\"150\",\"150\"]"
        )
        dao.saveSession(session1)
        assertTrue(dao.hasActiveSession())
        
        // Replace with second session
        val session2 = PlayerSessionEntity(
            id = 0,
            sessionId = "session-2",
            numOfRows = 3,
            playerNames = "[\"PlayerA\",\"PlayerB\",\"PlayerC\"]",
            buyAmounts = "[\"300\",\"400\",\"500\"]",
            returnAmounts = "[\"400\",\"400\",\"400\"]"
        )
        dao.saveSession(session2)
        
        // Verify second session data
        val retrieved = dao.getCurrentSessionOnce()
        assertEquals("session-2", retrieved?.sessionId)
        
        // Clear and verify
        dao.clearSession()
        assertFalse(dao.hasActiveSession())
        assertNull(dao.getCurrentSessionOnce())
    }
}