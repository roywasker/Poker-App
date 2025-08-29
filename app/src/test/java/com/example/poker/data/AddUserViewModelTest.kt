package com.example.poker.data

import android.util.Log
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

    // Tests for getPlayerList
    @Test
    fun `getPlayerList should load and update playerList`() = runTest {
        val mockUsers = listOf("Player1", "Player2", "Player3")
        coEvery { userRepository.getUserList() } returns mockUsers
        
        viewModel.getPlayerList()
        
        assertEquals(mockUsers, viewModel.playerList)
    }

    @Test
    fun `getPlayerList should handle empty list`() = runTest {
        coEvery { userRepository.getUserList() } returns emptyList()
        
        viewModel.getPlayerList()
        
        assertTrue(viewModel.playerList.isEmpty())
    }

    @Test
    fun `getPlayerList should handle exception and show error message`() = runTest {
        coEvery { userRepository.getUserList() } throws Exception("Network error")
        
        viewModel.getPlayerList()
        
        assertEquals("Failed to load player list", viewModel.messageDialog.value)
    }

    @Test
    fun `getPlayerList should clear existing list before adding new users`() = runTest {
        val initialUsers = listOf("OldPlayer1", "OldPlayer2")
        val newUsers = listOf("NewPlayer1", "NewPlayer2", "NewPlayer3")
        
        // Set initial users
        viewModel.playerList.addAll(initialUsers)
        
        coEvery { userRepository.getUserList() } returns newUsers
        
        viewModel.getPlayerList()
        
        assertEquals(newUsers, viewModel.playerList)
        assertEquals(3, viewModel.playerList.size)
    }

    // Tests for addUser
    @Test
    fun `addUser should add valid user successfully`() = runTest {
        val userName = "ValidUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Player ValidUser added to the list.", viewModel.messageDialog.value)
        assertEquals("", viewModel.userName.value) // Should be cleared after successful add
    }

    @Test
    fun `addUser should reject duplicate user name`() = runTest {
        val userName = "ExistingUser"
        viewModel.playerList.add(userName)
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Player ExistingUser is already in the list.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject empty user name`() = runTest {
        viewModel.onUserNameChange("")
        viewModel.addUser()
        
        assertEquals("Please enter a name.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject blank user name`() = runTest {
        viewModel.onUserNameChange("   ")
        viewModel.addUser()
        
        assertEquals("Please enter a name.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject name longer than 50 characters`() = runTest {
        val longName = "a".repeat(51)
        
        viewModel.onUserNameChange(longName)
        viewModel.addUser()
        
        assertEquals("Name is too long. Maximum 50 characters.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should reject name shorter than 2 characters`() = runTest {
        viewModel.onUserNameChange("A")
        viewModel.addUser()
        
        assertEquals("Name is too short. Minimum 2 characters.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `addUser should handle repository returning false`() = runTest {
        val userName = "NewUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns false
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Player NewUser is already in the list.", viewModel.messageDialog.value)
        assertEquals(userName, viewModel.userName.value) // Should not be cleared on failure
    }

    @Test
    fun `addUser should handle repository exception`() = runTest {
        val userName = "TestUser"
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } throws Exception("Database error")
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Failed to add player TestUser\n Try later", viewModel.messageDialog.value)
    }

    @Test
    fun `addUser should refresh player list after successful add`() = runTest {
        val userName = "NewPlayer"
        val updatedList = listOf("Player1", "Player2", userName)
        
        coEvery { userRepository.addUser(userName, 0) } returns true
        coEvery { userRepository.getUserList() } returns updatedList
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals(updatedList, viewModel.playerList)
        assertTrue(viewModel.playerList.contains(userName))
    }

    // Tests for changeUserValue
    @Test
    fun `changeUserValue should update balance successfully`() = runTest {
        val userName = "TestUser"
        val newBalance = 100
        val existingUsers = listOf(userName, "OtherUser")
        
        coEvery { userRepository.getUserList() } returns existingUsers
        coEvery { userRepository.changeUserBalance(userName, newBalance) } returns true
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = newBalance.toString()
        
        viewModel.changeUserValue()
        
        assertEquals("Balance successfully updated", viewModel.messageDialog.value)
        assertEquals("", viewModel.userToChange.value)
        assertEquals("", viewModel.moneyChange.value)
    }

    @Test
    fun `changeUserValue should reject empty money field`() = runTest {
        viewModel.userToChange.value = "TestUser"
        viewModel.moneyChange.value = ""
        
        viewModel.changeUserValue()
        
        assertEquals("Enter all the field ", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `changeUserValue should reject empty user field`() = runTest {
        viewModel.userToChange.value = ""
        viewModel.moneyChange.value = "100"
        
        viewModel.changeUserValue()
        
        assertEquals("Enter all the field ", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `changeUserValue should reject blank fields`() = runTest {
        viewModel.userToChange.value = "   "
        viewModel.moneyChange.value = "100"
        
        viewModel.changeUserValue()
        
        assertEquals("Enter all the field ", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `changeUserValue should reject invalid number format`() = runTest {
        viewModel.userToChange.value = "TestUser"
        viewModel.moneyChange.value = "abc"
        
        viewModel.changeUserValue()
        
        assertEquals("Please enter a valid number", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `changeUserValue should reject negative amount`() = runTest {
        viewModel.userToChange.value = "TestUser"
        viewModel.moneyChange.value = "-50"
        
        viewModel.changeUserValue()
        
        assertEquals("It is not possible to subtract a negative amount from a user.", viewModel.messageDialog.value)
        // No repository calls should be made since validation fails
    }

    @Test
    fun `changeUserValue should handle non-existent user`() = runTest {
        val userName = "NonExistentUser"
        val existingUsers = listOf("User1", "User2")
        
        coEvery { userRepository.getUserList() } returns existingUsers
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = "100"
        
        viewModel.changeUserValue()
        
        assertEquals("User not found", viewModel.messageDialog.value)
        // User not found, so changeUserBalance shouldn't be called
    }

    @Test
    fun `changeUserValue should handle repository returning false`() = runTest {
        val userName = "TestUser"
        val existingUsers = listOf(userName)
        
        coEvery { userRepository.getUserList() } returns existingUsers
        coEvery { userRepository.changeUserBalance(userName, 100) } returns false
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = "100"
        
        viewModel.changeUserValue()
        
        assertEquals("Failed to update user balance", viewModel.messageDialog.value)
        assertEquals(userName, viewModel.userToChange.value) // Should not be cleared on failure
        assertEquals("100", viewModel.moneyChange.value) // Should not be cleared on failure
    }

    @Test
    fun `changeUserValue should handle repository exception`() = runTest {
        val userName = "TestUser"
        val existingUsers = listOf(userName)
        
        coEvery { userRepository.getUserList() } returns existingUsers
        coEvery { userRepository.changeUserBalance(userName, 100) } throws Exception("Database error")
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = "100"
        
        viewModel.changeUserValue()
        
        assertEquals("Failed to update balance", viewModel.messageDialog.value)
    }

    @Test
    fun `changeUserValue should accept zero amount`() = runTest {
        val userName = "TestUser"
        val existingUsers = listOf(userName)
        
        coEvery { userRepository.getUserList() } returns existingUsers
        coEvery { userRepository.changeUserBalance(userName, 0) } returns true
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = "0"
        
        viewModel.changeUserValue()
        
        assertEquals("Balance successfully updated", viewModel.messageDialog.value)
    }

    @Test
    fun `changeUserValue should handle large numbers`() = runTest {
        val userName = "TestUser"
        val largeAmount = 1000000
        val existingUsers = listOf(userName)
        
        coEvery { userRepository.getUserList() } returns existingUsers
        coEvery { userRepository.changeUserBalance(userName, largeAmount) } returns true
        
        viewModel.userToChange.value = userName
        viewModel.moneyChange.value = largeAmount.toString()
        
        viewModel.changeUserValue()
        
        assertEquals("Balance successfully updated", viewModel.messageDialog.value)
    }

    // Edge case tests
    @Test
    fun `addUser should handle concurrent modifications to playerList`() = runTest {
        val userName = "NewUser"
        
        coEvery { userRepository.getUserList() } returns listOf("ExistingUser")
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        
        // Simulate another user being added to the list
        viewModel.playerList.add("AnotherUser")
        
        viewModel.addUser()
        
        assertEquals("Player NewUser added to the list.", viewModel.messageDialog.value)
    }

    @Test
    fun `multiple rapid calls to getPlayerList should handle correctly`() = runTest {
        val users1 = listOf("User1")
        val users2 = listOf("User1", "User2")
        val users3 = listOf("User1", "User2", "User3")
        
        coEvery { userRepository.getUserList() } returnsMany listOf(users1, users2, users3)
        
        // Make rapid calls
        viewModel.getPlayerList()
        viewModel.getPlayerList()
        viewModel.getPlayerList()
        
        // Final state should be users3
        assertEquals(users3, viewModel.playerList)
    }

    @Test
    fun `addUser with exactly 50 character name should succeed`() = runTest {
        val userName = "a".repeat(50)
        
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Player $userName added to the list.", viewModel.messageDialog.value)
    }

    @Test
    fun `addUser with exactly 2 character name should succeed`() = runTest {
        val userName = "AB"
        
        coEvery { userRepository.getUserList() } returns emptyList()
        coEvery { userRepository.addUser(userName, 0) } returns true
        
        viewModel.onUserNameChange(userName)
        viewModel.addUser()
        
        assertEquals("Player AB added to the list.", viewModel.messageDialog.value)
    }
}