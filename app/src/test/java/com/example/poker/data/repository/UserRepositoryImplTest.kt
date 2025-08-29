package com.example.poker.data.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import io.mockk.mockkStatic
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private lateinit var repository: UserRepositoryImpl
    private lateinit var playersRef: DatabaseReference
    private lateinit var mockSnapshot: DataSnapshot

    @Before
    fun setup() {
        playersRef = mockk(relaxed = true)
        mockSnapshot = mockk(relaxed = true)
        repository = UserRepositoryImpl(playersRef)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        
        // Mock kotlinx.coroutines.tasks.await extension
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // Tests for addUser
    @Test
    fun `addUser should return true when adding new user successfully`() = runTest {
        val userName = "NewUser"
        val initialBalance = 100
        val playerId = "generatedId123"
        val newUserRef = mockk<DatabaseReference>(relaxed = true)
        
        // Mock empty existing users
        every { mockSnapshot.children } returns emptyList<DataSnapshot>()
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        // Mock successful user creation
        every { playersRef.push() } returns newUserRef
        every { newUserRef.key } returns playerId
        every { playersRef.child(playerId) } returns newUserRef
        every { newUserRef.setValue(any()) } returns Tasks.forResult(null)
        
        val result = repository.addUser(userName, initialBalance)
        
        assertTrue(result)
        
        // Verify the correct data was saved
        val dataSlot = slot<Map<String, Any>>()
        verify { newUserRef.setValue(capture(dataSlot)) }
        assertEquals(userName, dataSlot.captured["name"])
        assertEquals(initialBalance, dataSlot.captured["balance"])
    }

    @Test
    fun `addUser should return false when user already exists`() = runTest {
        val existingUserName = "ExistingUser"
        val mockUserSnapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { mockUserSnapshot.child("name").getValue(String::class.java) } returns existingUserName
        every { mockSnapshot.children } returns listOf(mockUserSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.addUser(existingUserName, 100)
        
        assertFalse(result)
    }

    @Test
    fun `addUser should return false when push key is null`() = runTest {
        val userName = "NewUser"
        val newUserRef = mockk<DatabaseReference>(relaxed = true)
        
        every { mockSnapshot.children } returns emptyList<DataSnapshot>()
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        every { playersRef.push() } returns newUserRef
        every { newUserRef.key } returns null
        
        val result = repository.addUser(userName, 100)
        
        assertFalse(result)
    }

    @Test
    fun `addUser should return false and log error on exception`() = runTest {
        val userName = "NewUser"
        
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.addUser(userName, 100)
        
        assertFalse(result)
        verify { Log.e("ContentValues", "addUser: java.lang.Exception: Database error") }
    }

    // Tests for getUserList
    @Test
    fun `getUserList should return list of all users`() = runTest {
        val user1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { user1Snapshot.child("name").getValue(String::class.java) } returns "User1"
        every { user2Snapshot.child("name").getValue(String::class.java) } returns "User2"
        every { user3Snapshot.child("name").getValue(String::class.java) } returns "User3"
        
        every { mockSnapshot.children } returns listOf(user1Snapshot, user2Snapshot, user3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getUserList()
        
        assertEquals(listOf("User1", "User2", "User3"), result)
    }

    @Test
    fun `getUserList should skip users with null names`() = runTest {
        val user1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user3Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { user1Snapshot.child("name").getValue(String::class.java) } returns "User1"
        every { user2Snapshot.child("name").getValue(String::class.java) } returns null
        every { user3Snapshot.child("name").getValue(String::class.java) } returns "User3"
        
        every { mockSnapshot.children } returns listOf(user1Snapshot, user2Snapshot, user3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getUserList()
        
        assertEquals(listOf("User1", "User3"), result)
    }

    @Test
    fun `getUserList should return empty list when no users exist`() = runTest {
        every { mockSnapshot.children } returns emptyList<DataSnapshot>()
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.getUserList()
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUserList should return empty list and log error on exception`() = runTest {
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.getUserList()
        
        assertTrue(result.isEmpty())
        verify { Log.e("ContentValues", "getUserList: java.lang.Exception: Database error") }
    }

    // Tests for changeUserBalance
    @Test
    fun `changeUserBalance should return true when balance updated successfully`() = runTest {
        val userName = "TestUser"
        val newBalance = 500
        val userSnapshot = mockk<DataSnapshot>(relaxed = true)
        val userRef = mockk<DatabaseReference>(relaxed = true)
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        
        every { userSnapshot.child("name").getValue(String::class.java) } returns userName
        every { userSnapshot.ref } returns userRef
        every { userRef.child("balance") } returns balanceRef
        every { balanceRef.setValue(newBalance) } returns Tasks.forResult(null)
        
        every { mockSnapshot.children } returns listOf(userSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.changeUserBalance(userName, newBalance)
        
        assertTrue(result)
        verify { balanceRef.setValue(newBalance) }
    }

    @Test
    fun `changeUserBalance should return false when user not found`() = runTest {
        val userName = "NonExistentUser"
        val user1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user2Snapshot = mockk<DataSnapshot>(relaxed = true)
        
        every { user1Snapshot.child("name").getValue(String::class.java) } returns "User1"
        every { user2Snapshot.child("name").getValue(String::class.java) } returns "User2"
        
        every { mockSnapshot.children } returns listOf(user1Snapshot, user2Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.changeUserBalance(userName, 500)
        
        assertFalse(result)
    }

    @Test
    fun `changeUserBalance should handle multiple users and update correct one`() = runTest {
        val targetUser = "TargetUser"
        val newBalance = 1000
        val user1Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user2Snapshot = mockk<DataSnapshot>(relaxed = true)
        val user3Snapshot = mockk<DataSnapshot>(relaxed = true)
        val targetRef = mockk<DatabaseReference>(relaxed = true)
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        
        every { user1Snapshot.child("name").getValue(String::class.java) } returns "User1"
        every { user2Snapshot.child("name").getValue(String::class.java) } returns targetUser
        every { user3Snapshot.child("name").getValue(String::class.java) } returns "User3"
        
        every { user2Snapshot.ref } returns targetRef
        every { targetRef.child("balance") } returns balanceRef
        every { balanceRef.setValue(newBalance) } returns Tasks.forResult(null)
        
        every { mockSnapshot.children } returns listOf(user1Snapshot, user2Snapshot, user3Snapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.changeUserBalance(targetUser, newBalance)
        
        assertTrue(result)
        verify { balanceRef.setValue(newBalance) }
    }

    @Test
    fun `changeUserBalance should return false and log error on exception`() = runTest {
        val userName = "TestUser"
        
        every { playersRef.get() } throws Exception("Database error")
        
        val result = repository.changeUserBalance(userName, 500)
        
        assertFalse(result)
        verify { Log.e("ContentValues", "changeUserBalance: java.lang.Exception: Database error") }
    }

    @Test
    fun `changeUserBalance should handle zero balance`() = runTest {
        val userName = "TestUser"
        val newBalance = 0
        val userSnapshot = mockk<DataSnapshot>(relaxed = true)
        val userRef = mockk<DatabaseReference>(relaxed = true)
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        
        every { userSnapshot.child("name").getValue(String::class.java) } returns userName
        every { userSnapshot.ref } returns userRef
        every { userRef.child("balance") } returns balanceRef
        every { balanceRef.setValue(newBalance) } returns Tasks.forResult(null)
        
        every { mockSnapshot.children } returns listOf(userSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.changeUserBalance(userName, newBalance)
        
        assertTrue(result)
        verify { balanceRef.setValue(0) }
    }

    @Test
    fun `changeUserBalance should handle negative balance`() = runTest {
        val userName = "TestUser"
        val newBalance = -100
        val userSnapshot = mockk<DataSnapshot>(relaxed = true)
        val userRef = mockk<DatabaseReference>(relaxed = true)
        val balanceRef = mockk<DatabaseReference>(relaxed = true)
        
        every { userSnapshot.child("name").getValue(String::class.java) } returns userName
        every { userSnapshot.ref } returns userRef
        every { userRef.child("balance") } returns balanceRef
        every { balanceRef.setValue(newBalance) } returns Tasks.forResult(null)
        
        every { mockSnapshot.children } returns listOf(userSnapshot)
        every { playersRef.get() } returns Tasks.forResult(mockSnapshot)
        
        val result = repository.changeUserBalance(userName, newBalance)
        
        assertTrue(result)
        verify { balanceRef.setValue(-100) }
    }
}