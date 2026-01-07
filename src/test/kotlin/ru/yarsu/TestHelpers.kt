package ru.yarsu

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.server.testing.ApplicationTestBuilder

/**
 * Test utilities and helper functions for FlipSpace tests
 */
object TestHelpers {

    /**
     * Generates a unique username for testing to avoid conflicts
     */
    fun generateUniqueUsername(prefix: String = "testuser"): String {
        return "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Registers a new test user and returns the username
     */
    suspend fun ApplicationTestBuilder.registerTestUser(
        username: String? = null,
        password: String = "password123",
    ): String {
        val testUsername = username ?: generateUniqueUsername()

        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", password)
                append("confirm_password", password)
            },
        )

        return testUsername
    }

    /**
     * Registers and logs in a test user, returning the authenticated client
     */
    suspend fun ApplicationTestBuilder.loginTestUser(
        username: String? = null,
        password: String = "password123",
    ): Pair<HttpClient, String> {
        val testUsername = registerTestUser(username, password)

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", password)
            },
        )

        return Pair(client, testUsername)
    }

    /**
     * Common test passwords
     */
    object TestPasswords {
        const val VALID = "password123"
        const val SHORT = "12345" // Too short (< 6)
        const val VERY_SHORT = "123" // Very short
        const val STRONG = "SuperSecure123!"
    }

    /**
     * Common test usernames
     */
    object TestUsernames {
        const val SHORT = "ab" // Too short (< 3)
        const val VALID = "testuser"
        const val VALID_MIN = "abc" // Minimum valid length
    }
}
