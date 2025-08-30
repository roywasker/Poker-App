package com.example.poker.data.viewmodel

import android.util.Log
import com.example.poker.data.AddUserViewModel
import com.example.poker.data.repository.UserRepository
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
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddUserViewModelTest {

    private lateinit var viewModel: AddUserViewModel
    private lateinit var userRepository: UserRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        viewModel = AddUserViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Tests for onUserNameChange
    @Test
    fun `onUserNameChange should update userName value`() {
        val newName = "TestUser"
        
        viewModel.onUserNameChange(newName)
        
        assertEquals(newName, viewModel.userName.value)
    }

    @Test
    fun `onUserNameChange should handle empty string`() {
        viewModel.onUserNameChange("")
        
        assertEquals("", viewModel.userName.value)
    }

    // Tests for clearMessageDialog
    @Test
    fun `clearMessageDialog should set messageDialog to null`() = runTest {
        // First set a message
        viewModel.onUserNameChange("") // Ensure userName is empty
        viewModel.addUser() // This will set an error message since userName is empty
        
        assertNotNull(viewModel.messageDialog.value)
        
        viewModel.clearMessageDialog()
        
        assertNull(viewModel.messageDialog.value)
    }

    // Tests for refreshPlayerList / getUserList
    @Test
    fun `refreshPlayerList should load and update playerList`() = runTest {
        val mockUsers = listOf("Player1", "Player2", "Player3")
        coEvery { userRepository.getUserList() } returns mockUsers
        
        viewModel.refreshPlayerList()
        
        assertEquals(mockUsers, viewModel.playerList.value)
    }

    @Test
    fun `refreshPlayerList should handle empty list`() = runTest {
        coEvery { userRepository.getUserList() } returns emptyList()
        
        viewModel.refreshPlayerList()
        
        assertTrue(viewModel.playerList.value.isEmpty())
    }

    @Test
    fun `refreshPlayerList should handle exception and show error message`() = runTest {
        coEvery { userRepository.getUserList() } throws Exception("Network error")
        
        viewModel.refreshPlayerList()
        
        assertEquals("Failed to load players: Network error", viewModel.messageDialog.value)
    }

    @Test
    fun `refreshPlayerList should update to new users`() = runTest {
        val newUsers = listOf("NewPlayer1", "NewPlayer2", "NewPlayer3")
        
        coEvery { userRepository.getUserList() } returns newUsers
        
        viewModel.refreshPlayerList()
        
        assertEquals(newUsers.sorted(), viewModel.playerList.value)
        assertEquals(3, viewModel.playerList.value.size)
    }

    // Tests for addUser
    @Test
    fun `addUser should add valid user successfully`() = runTest {
        val userName = "ValidUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("User 'ValidUser' added successfully", viewModel.messageDialog.value)
        assertEquals("", viewModel.userName.value) // Should be cleared after successful add
    }

    @Test
    fun `addUser should reject duplicate user name`() = runTest {
        val userName = "ExistingUser"
        coEvery { userRepository.getUserList() } returns listOf(userName)
        
        // Load the initial player list
        viewModel.refreshPlayerList()
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("User 'ExistingUser' already exists", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject empty user name`() = runTest {
        viewModel.onUserNameChange("")
        viewModel.addUser()
        
        assertEquals("Username cannot be empty", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject blank user name`() = runTest {
        viewModel.onUserNameChange("   ")
        viewModel.addUser()
        
        assertEquals("Username cannot be empty", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should handle name longer than 50 characters by truncating`() = runTest {
        val longName = "a".repeat(51)
        val truncatedName = "a".repeat(50)
        
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(truncatedName, 0) } returns true
        
        viewModel.onUserNameChange(longName)
        // Note: sanitizeInput truncates to 50 chars, so this will actually pass validation
        // The viewModel username becomes "a" * 50 after sanitization
        viewModel.addUser()
        
        assertEquals("User '$truncatedName' added successfully", viewModel.messageDialog.value)
    }

    @Test
    fun `addUser should reject name shorter than 2 characters`() = runTest {
        viewModel.onUserNameChange("A")
        viewModel.addUser()
        
        assertEquals("Username too short (minimum 2 characters)", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should handle repository returning false`() = runTest {
        val userName = "NewUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns false
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Failed to add user. User may already exist.", viewModel.messageDialog.value)
        assertEquals(userName, viewModel.userName.value) // Should not be cleared on failure
    }

    @Test
    fun `addUser should handle repository exception`() = runTest {
        val userName = "TestUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } throws Exception("Database error")
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Failed to add user: Database error", viewModel.messageDialog.value)
    }

    @Test
    fun `addUser should refresh player list after successful add`() = runTest {
        val userName = "NewPlayer"
        val updatedList = listOf("NewPlayer", "Player1", "Player2")
        
        // The init block already called getUserList() once during setup which returns emptyList (default mock)
        // So we need to setup the return for the call after addUser succeeds
        coEvery { userRepository.addUser(userName, 0) } returns true
        coEvery { userRepository.getUserList() } returns updatedList
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        // The list should be sorted
        assertEquals(updatedList.sorted(), viewModel.playerList.value)
        assertTrue(viewModel.playerList.value.contains(userName))
    }



    // Edge case tests
    @Test
    fun `addUser should handle concurrent modifications to playerList`() = runTest {
        val userName = "NewUser"
        
        coEvery { userRepository.getUserList() } returns listOf("ExistingUser")
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        
        // The playerList is now immutable StateFlow, so we can't add directly
        // We need to trigger a refresh to simulate the concurrent modification
        viewModel.refreshPlayerList()
        
        viewModel.addUser()
        
        assertEquals("User 'NewUser' added successfully", viewModel.messageDialog.value)
    }

    @Test
    fun `multiple rapid calls to refreshPlayerList should handle correctly`() = runTest {
        val users1 = listOf("User1")
        val users2 = listOf("User1", "User2")
        val users3 = listOf("User1", "User2", "User3")
        
        coEvery { userRepository.getUserList() } returnsMany listOf(users1, users2, users3)
        
        // Make rapid calls
        viewModel.refreshPlayerList()
        viewModel.refreshPlayerList()
        viewModel.refreshPlayerList()
        
        // Final state should be users3 (sorted)
        assertEquals(users3.sorted(), viewModel.playerList.value)
    }

    @Test
    fun `addUser with exactly 50 character name should succeed`() = runTest {
        val userName = "a".repeat(50)
        
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("User '$userName' added successfully", viewModel.messageDialog.value)
    }

    @Test
    fun `addUser with exactly 2 character name should succeed`() = runTest {
        val userName = "AB"
        
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("User 'AB' added successfully", viewModel.messageDialog.value)
    }

    // Network timeout tests
    @Test
    fun `refreshPlayerList should handle timeout and show network error`() = runTest {
        coEvery { userRepository.getUserList() } throws Exception("Request timeout")
        
        viewModel.refreshPlayerList()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout. Please check your internet connection.", viewModel.networkError.value?.message)
        
        // Player list should be empty due to timeout
        assertTrue(viewModel.playerList.value.isEmpty())
    }

    @Test
    fun `addUser should handle timeout and show network error`() = runTest {
        val userName = "TestUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } throws Exception("Request timeout")
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        // Should show network error
        assertNotNull(viewModel.networkError.value)
        assertEquals("Connection timeout while adding user. Please check your internet connection.", viewModel.networkError.value?.message)
    }

    @Test
    fun `network error retry should work correctly`() = runTest {
        var callCount = 0
        coEvery { userRepository.getUserList() } answers {
            callCount++
            if (callCount == 1) {
                throw Exception("Request timeout")
            } else {
                listOf("Player1", "Player2") // Second call succeeds
            }
        }
        
        // First call - will timeout
        viewModel.refreshPlayerList()
        assertNotNull(viewModel.networkError.value)
        
        // Retry
        viewModel.retryNetworkOperation()
        
        // Should now have players
        assertEquals(listOf("Player1", "Player2"), viewModel.playerList.value)
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `clearNetworkError should clear error state`() = runTest {
        coEvery { userRepository.getUserList() } throws Exception("Request timeout")
        
        viewModel.refreshPlayerList()
        assertNotNull(viewModel.networkError.value)
        
        viewModel.clearNetworkError()
        assertNull(viewModel.networkError.value)
    }

    @Test
    fun `successful network call should clear any existing network error`() = runTest {
        // First set up a timeout error
        coEvery { userRepository.getUserList() } throws Exception("Request timeout")
        viewModel.refreshPlayerList()
        assertNotNull(viewModel.networkError.value)
        
        // Now make a successful call
        coEvery { userRepository.getUserList() } returns listOf("Player1")
        viewModel.refreshPlayerList()
        
        // Network error should be cleared
        assertNull(viewModel.networkError.value)
        assertEquals(listOf("Player1"), viewModel.playerList.value)
    }
}