package ru.yarsu.services

import at.favre.lib.crypto.bcrypt.BCrypt
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import ru.yarsu.db.DatabaseService
import ru.yarsu.db.User
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private fun createMockDatabaseService(): DatabaseService {
        return mockk<DatabaseService>(relaxed = true)
    }

    @Test
    fun `registerUser with valid credentials should succeed`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val testLogin = "testuser"
        val testPassword = "password123"

        every { dbService.getUserByLogin(testLogin) } returns null
        every { dbService.createUser(any(), any()) } returns "generated-user-id"

        // Act
        val result = authService.registerUser(testLogin, testPassword)

        // Assert
        assertTrue(result.isSuccess, "Registration should succeed with valid credentials")
        assertEquals("generated-user-id", result.getOrNull())

        verify(exactly = 1) { dbService.getUserByLogin(testLogin) }
        verify(exactly = 1) { dbService.createUser(eq(testLogin), any()) }
    }

    @Test
    fun `registerUser with duplicate login should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val existingUser = User(
            id = "existing-id",
            login = "existinguser",
            password = "hashed",
        )

        every { dbService.getUserByLogin("existinguser") } returns existingUser

        // Act
        val result = authService.registerUser("existinguser", "password123")

        // Assert
        assertTrue(result.isFailure, "Registration should fail with duplicate login")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("уже существует") == true,
            "Error message should indicate login is already in use",
        )

        verify(exactly = 1) { dbService.getUserByLogin("existinguser") }
        verify(exactly = 0) { dbService.createUser(any(), any()) }
    }

    @Test
    fun `registerUser with short login should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val shortLogin = "ab" // Less than 3 characters

        // Act
        val result = authService.registerUser(shortLogin, "password123")

        // Assert
        assertTrue(result.isFailure, "Registration should fail with short login")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("3 символа") == true,
            "Error message should mention minimum 3 characters",
        )

        verify(exactly = 0) { dbService.getUserByLogin(any()) }
        verify(exactly = 0) { dbService.createUser(any(), any()) }
    }

    @Test
    fun `registerUser with blank login should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)

        // Act
        val result = authService.registerUser("", "password123")

        // Assert
        assertTrue(result.isFailure, "Registration should fail with blank login")
        verify(exactly = 0) { dbService.createUser(any(), any()) }
    }

    @Test
    fun `registerUser with short password should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val shortPassword = "12345" // Less than 6 characters

        // Act
        val result = authService.registerUser("validlogin", shortPassword)

        // Assert
        assertTrue(result.isFailure, "Registration should fail with short password")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("6 символов") == true,
            "Error message should mention minimum 6 characters",
        )

        verify(exactly = 0) { dbService.createUser(any(), any()) }
    }

    @Test
    fun `registerUser with blank password should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)

        // Act
        val result = authService.registerUser("validlogin", "")

        // Assert
        assertTrue(result.isFailure, "Registration should fail with blank password")
        verify(exactly = 0) { dbService.createUser(any(), any()) }
    }

    @Test
    fun `registerUser should hash password with BCrypt`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val passwordSlot = slot<String>()

        every { dbService.getUserByLogin(any()) } returns null
        every { dbService.createUser(any(), capture(passwordSlot)) } returns "user-id"

        // Act
        authService.registerUser("testuser", "password123")

        // Assert
        val capturedPasswordHash = passwordSlot.captured
        assertNotNull(capturedPasswordHash, "Password hash should be captured")
        assertTrue(
            BCrypt.verifyer().verify("password123".toCharArray(), capturedPasswordHash).verified,
            "Stored password should be BCrypt hashed",
        )
    }

    @Test
    fun `authenticateUser with correct password should succeed`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val testPassword = "password123"
        val hashedPassword = BCrypt.withDefaults().hashToString(12, testPassword.toCharArray())

        val testUser = User(
            id = "test-id",
            login = "testuser",
            password = hashedPassword,
        )

        every { dbService.getUserByLogin("testuser") } returns testUser

        // Act
        val result = authService.authenticateUser("testuser", testPassword)

        // Assert
        assertTrue(result.isSuccess, "Authentication should succeed with correct password")
        assertEquals(testUser, result.getOrNull())

        verify(exactly = 1) { dbService.getUserByLogin("testuser") }
    }

    @Test
    fun `authenticateUser with incorrect password should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val correctPassword = "password123"
        val incorrectPassword = "wrongpassword"
        val hashedPassword = BCrypt.withDefaults().hashToString(12, correctPassword.toCharArray())

        val testUser = User(
            id = "test-id",
            login = "testuser",
            password = hashedPassword,
        )

        every { dbService.getUserByLogin("testuser") } returns testUser

        // Act
        val result = authService.authenticateUser("testuser", incorrectPassword)

        // Assert
        assertTrue(result.isFailure, "Authentication should fail with incorrect password")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Неверный логин или пароль") == true,
            "Error message should indicate invalid credentials",
        )
    }

    @Test
    fun `authenticateUser with non-existent user should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)

        every { dbService.getUserByLogin("nonexistent") } returns null

        // Act
        val result = authService.authenticateUser("nonexistent", "password123")

        // Assert
        assertTrue(result.isFailure, "Authentication should fail with non-existent user")
        assertTrue(
            result.exceptionOrNull()?.message?.contains("Неверный логин или пароль") == true,
        )
    }

    @Test
    fun `validateSession with existing user should succeed`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)
        val testUser = User(
            id = "test-id",
            login = "testuser",
            password = "hashed",
        )

        every { dbService.getUserById("test-id") } returns testUser

        // Act
        val result = authService.validateSession("test-id")

        // Assert
        assertNotNull(result, "Session validation should return user for existing user")
        assertEquals(testUser, result)
        verify(exactly = 1) { dbService.getUserById("test-id") }
    }

    @Test
    fun `validateSession with non-existent user should fail`() {
        // Arrange
        val dbService = createMockDatabaseService()
        val authService = AuthService(dbService)

        every { dbService.getUserById("invalid-id") } returns null

        // Act
        val result = authService.validateSession("invalid-id")

        // Assert
        assertTrue(result == null, "Session validation should return null for non-existent user")
    }

    @Test
    fun `BCrypt hashing should be consistent and verifiable`() {
        // This test verifies BCrypt functionality itself
        val password = "testpassword123"
        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        // Same password should verify against hash
        assertTrue(
            actual = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified,
            message = "Same password should verify against its hash",
        )

        // Different password should not verify
        assertFalse(
            actual = BCrypt.verifyer().verify("wrongpassword".toCharArray(), hashedPassword).verified,
            message = "Different password should not verify against hash",
        )
    }
}
