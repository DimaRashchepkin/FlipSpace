package ru.yarsu.services

import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.yarsu.TestDatabaseConnection
import ru.yarsu.db.UserDatabaseService
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserDatabaseServiceTest {

    private lateinit var connection: Connection
    private lateinit var userDatabaseService: UserDatabaseService

    @Before
    fun setup() {
        connection = TestDatabaseConnection.getConnection()
        userDatabaseService = UserDatabaseService(connection)

        // Clean up users table before each test
        connection.createStatement().execute("DELETE FROM users")
    }

    @After
    fun tearDown() {
        // Clean up after tests
        connection.createStatement().execute("DELETE FROM users")
    }

    @Test
    fun `createUser should create user and return ID`() {
        // Arrange
        val login = "testuser"
        val password = "password123"

        // Act
        val userId = userDatabaseService.createUser(login, password)

        // Assert
        assertNotNull(userId, "User ID should not be null")
        assertTrue(userId.isNotEmpty(), "User ID should not be empty")

        // Verify user was actually created
        val createdUser = userDatabaseService.getUserById(userId)
        assertNotNull(createdUser)
        assertEquals(login, createdUser.login)
        assertEquals(password, createdUser.password)
    }

    @Test
    fun `createUser should generate unique IDs for multiple users`() {
        // Arrange & Act
        val userId1 = userDatabaseService.createUser("user1", "pass1")
        val userId2 = userDatabaseService.createUser("user2", "pass2")

        // Assert
        assertTrue(userId1 != userId2, "User IDs should be unique")
    }

    @Test
    fun `getUserById should return user when it exists`() {
        // Arrange
        val login = "findme"
        val password = "secret123"
        val userId = userDatabaseService.createUser(login, password)

        // Act
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user, "User should be found")
        assertEquals(userId, user.id)
        assertEquals(login, user.login)
        assertEquals(password, user.password)
    }

    @Test
    fun `getUserById should return null when user does not exist`() {
        // Act
        val user = userDatabaseService.getUserById("non-existent-id")

        // Assert
        assertNull(user, "Should return null for non-existent user")
    }

    @Test
    fun `getUserByLogin should return user when login exists`() {
        // Arrange
        val login = "uniquelogin"
        val password = "mypassword"
        userDatabaseService.createUser(login, password)

        // Act
        val user = userDatabaseService.getUserByLogin(login)

        // Assert
        assertNotNull(user, "User should be found by login")
        assertEquals(login, user.login)
        assertEquals(password, user.password)
    }

    @Test
    fun `getUserByLogin should return null when login does not exist`() {
        // Act
        val user = userDatabaseService.getUserByLogin("nonexistent")

        // Assert
        assertNull(user, "Should return null for non-existent login")
    }

    @Test
    fun `getUserByLogin should be case-sensitive`() {
        // Arrange
        userDatabaseService.createUser("TestUser", "password")

        // Act
        val user1 = userDatabaseService.getUserByLogin("TestUser")
        val user2 = userDatabaseService.getUserByLogin("testuser")

        // Assert
        assertNotNull(user1, "Should find exact match")
        assertNull(user2, "Should not find different case")
    }

    @Test
    fun `getAllUsers should return all users`() {
        // Arrange
        userDatabaseService.createUser("user1", "pass1")
        userDatabaseService.createUser("user2", "pass2")
        userDatabaseService.createUser("user3", "pass3")

        // Act
        val users = userDatabaseService.getAllUsers()

        // Assert
        assertEquals(3, users.size, "Should return 3 users")

        val logins = users.map { it.login }.toSet()
        assertTrue(logins.contains("user1"))
        assertTrue(logins.contains("user2"))
        assertTrue(logins.contains("user3"))
    }

    @Test
    fun `getAllUsers should return empty list when no users exist`() {
        // Act
        val users = userDatabaseService.getAllUsers()

        // Assert
        assertTrue(users.isEmpty(), "Should return empty list when no users exist")
    }

    @Test
    fun `createUser with special characters in login should work`() {
        // Arrange
        val specialLogin = "user_test-123"
        val password = "password"

        // Act
        val userId = userDatabaseService.createUser(specialLogin, password)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(specialLogin, user.login)
    }

    @Test
    fun `createUser with long login should work`() {
        // Arrange
        val longLogin = "a".repeat(50)
        val password = "password"

        // Act
        val userId = userDatabaseService.createUser(longLogin, password)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(longLogin, user.login)
    }

    @Test
    fun `createUser with long password should work`() {
        // Arrange
        val login = "testuser"
        val longPassword = "p".repeat(100)

        // Act
        val userId = userDatabaseService.createUser(login, longPassword)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(longPassword, user.password)
    }

    @Test
    fun `createUser with empty password should work`() {
        // Arrange
        val login = "testuser"
        val emptyPassword = ""

        // Act
        val userId = userDatabaseService.createUser(login, emptyPassword)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals("", user.password)
    }

    @Test
    fun `getAllUsers should return users in consistent order`() {
        // Arrange
        val userId1 = userDatabaseService.createUser("user1", "pass1")
        val userId2 = userDatabaseService.createUser("user2", "pass2")
        val userId3 = userDatabaseService.createUser("user3", "pass3")

        // Act
        val users1 = userDatabaseService.getAllUsers()
        val users2 = userDatabaseService.getAllUsers()

        // Assert
        assertEquals(users1.size, users2.size)
        assertEquals(users1.map { it.id }, users2.map { it.id }, "Order should be consistent")
    }

    @Test
    fun `getUserByLogin should handle users with same prefix`() {
        // Arrange
        userDatabaseService.createUser("test", "pass1")
        userDatabaseService.createUser("tester", "pass2")
        userDatabaseService.createUser("testing", "pass3")

        // Act
        val user = userDatabaseService.getUserByLogin("test")

        // Assert
        assertNotNull(user)
        assertEquals("test", user.login, "Should return exact match only")
    }

    @Test
    fun `createUser should handle Unicode characters in login`() {
        // Arrange
        val unicodeLogin = "пользователь123"
        val password = "password"

        // Act
        val userId = userDatabaseService.createUser(unicodeLogin, password)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(unicodeLogin, user.login)
    }

    @Test
    fun `createUser should handle Unicode characters in password`() {
        // Arrange
        val login = "testuser"
        val unicodePassword = "пароль密码🔒"

        // Act
        val userId = userDatabaseService.createUser(login, unicodePassword)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(unicodePassword, user.password)
    }

    @Test
    fun `getUserById with malformed UUID should return null`() {
        // Act
        val user = userDatabaseService.getUserById("not-a-uuid")

        // Assert
        assertNull(user, "Should handle malformed UUID gracefully")
    }

    @Test
    fun `createUser should prevent SQL injection in login`() {
        // Arrange
        val sqlInjectionLogin = "admin'; DROP TABLE users; --"
        val password = "password"

        // Act
        val userId = userDatabaseService.createUser(sqlInjectionLogin, password)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user, "Should create user with SQL injection string as literal text")
        assertEquals(sqlInjectionLogin, user.login)

        // Verify table still exists
        val allUsers = userDatabaseService.getAllUsers()
        assertFalse(allUsers.isEmpty(), "Users table should still exist")
    }

    @Test
    fun `getUserByLogin should prevent SQL injection`() {
        // Arrange
        userDatabaseService.createUser("admin", "password")
        val sqlInjection = "admin' OR '1'='1"

        // Act
        val user = userDatabaseService.getUserByLogin(sqlInjection)

        // Assert
        assertNull(user, "Should not return user with SQL injection attempt")
    }

    @Test
    fun `createUser with whitespace in login should work`() {
        // Arrange
        val loginWithSpaces = "user name"
        val password = "password"

        // Act
        val userId = userDatabaseService.createUser(loginWithSpaces, password)
        val user = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user)
        assertEquals(loginWithSpaces, user.login)
    }

    @Test
    fun `multiple concurrent getUserById calls should work correctly`() {
        // Arrange
        val userId = userDatabaseService.createUser("testuser", "password")

        // Act - simulate concurrent access
        val user1 = userDatabaseService.getUserById(userId)
        val user2 = userDatabaseService.getUserById(userId)

        // Assert
        assertNotNull(user1)
        assertNotNull(user2)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.login, user2.login)
    }
}
