package ru.yarsu.web.controllers

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication
import org.junit.Test
import ru.yarsu.testModule
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthControllerTest {

    @Test
    fun `GET register page should return 200`() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/register")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("register") || body.contains("Регистрация"), "Response should contain registration form")
    }

    @Test
    fun `GET login page should return 200`() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/login")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("login") || body.contains("Вход"), "Response should contain login form")
    }

    @Test
    fun `POST register with valid credentials should create user and redirect`() = testApplication {
        application {
            testModule()
        }

        val uniqueUsername = "testuser_${System.currentTimeMillis()}"
        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", uniqueUsername)
                append("password", "password123")
                append("confirm_password", "password123")
            },
        ) {
            // Не следуем редиректам автоматически
        }

        // Should redirect after successful registration
        assertTrue(
            response.status == HttpStatusCode.Found || response.status == HttpStatusCode.SeeOther,
            "Should redirect after successful registration, got ${response.status}",
        )
    }

    @Test
    fun `POST register with duplicate username should return error`() = testApplication {
        application {
            testModule()
        }

        val duplicateUsername = "duplicate_${System.currentTimeMillis()}"

        // First registration
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", duplicateUsername)
                append("password", "password123")
                append("confirm_password", "password123")
            },
        )

        // Second registration with same username
        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", duplicateUsername)
                append("password", "password456")
                append("confirm_password", "password456")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("уже существует") || body.contains("already"),
            "Response should indicate username is already in use",
        )
    }

    @Test
    fun `POST register with short username should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", "ab") // Less than 3 characters
                append("password", "password123")
                append("confirm_password", "password123")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("3") || body.contains("короткий"),
            "Response should indicate username is too short",
        )
    }

    @Test
    fun `POST register with short password should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", "validuser")
                append("password", "12345") // Less than 6 characters
                append("confirm_password", "12345")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("6") || body.contains("короткий"),
            "Response should indicate password is too short",
        )
    }

    @Test
    fun `POST register with mismatched passwords should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", "validuser")
                append("password", "password123")
                append("confirm_password", "different456")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("совпадают") || body.contains("match"),
            "Response should indicate passwords don't match",
        )
    }

    @Test
    fun `POST register with blank fields should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", "")
                append("password", "")
                append("confirm_password", "")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST login with correct credentials should succeed and set session`() = testApplication {
        application {
            testModule()
        }

        // First, register a user
        val testUsername = "logintest_${System.currentTimeMillis()}"
        val testPassword = "password123"

        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", testPassword)
                append("confirm_password", testPassword)
            },
        )

        // Then try to login
        val loginResponse = client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", testPassword)
            },
        )

        assertTrue(
            loginResponse.status == HttpStatusCode.Found || loginResponse.status == HttpStatusCode.SeeOther,
            "Should redirect after successful login, got ${loginResponse.status}",
        )

        // Verify we have a session by accessing protected route
        val setsResponse = client.get("/sets")
        assertEquals(
            HttpStatusCode.OK,
            setsResponse.status,
            "Should be able to access /sets with valid session",
        )
    }

    @Test
    fun `POST login with incorrect password should return error`() = testApplication {
        application {
            testModule()
        }

        // Register a user first
        val testUsername = "wrongpass_${System.currentTimeMillis()}"
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", "correctpassword")
                append("confirm_password", "correctpassword")
            },
        )

        // Try to login with wrong password
        val response = client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", "wrongpassword")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Неверный") || body.contains("Invalid"),
            "Response should indicate invalid credentials",
        )
    }

    @Test
    fun `POST login with non-existent user should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", "nonexistent_user_12345")
                append("password", "password123")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("Неверный") || body.contains("Invalid"),
            "Response should indicate invalid credentials",
        )
    }

    @Test
    fun `POST login with blank fields should return error`() = testApplication {
        application {
            testModule()
        }

        val response = client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", "")
                append("password", "")
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET logout should clear session and redirect to login`() = testApplication {
        application {
            testModule()
        }

        // Register and login first
        val testUsername = "logout_${System.currentTimeMillis()}"
        val testPassword = "password123"

        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", testPassword)
                append("confirm_password", testPassword)
            },
        )

        // Logout
        val logoutResponse = client.get("/logout")

        // After logout, either we get redirect status or client follows redirect to login page
        val isLogoutSuccess = logoutResponse.status == HttpStatusCode.Found ||
            logoutResponse.status == HttpStatusCode.SeeOther ||
            (logoutResponse.status == HttpStatusCode.OK && logoutResponse.bodyAsText().contains("login"))

        assertTrue(isLogoutSuccess, "Should redirect after logout")

        // Try to access protected route - should redirect to login or show login page
        val setsResponse = client.get("/sets")
        val isSetsBlocked = setsResponse.status == HttpStatusCode.Found ||
            setsResponse.status == HttpStatusCode.SeeOther ||
            setsResponse.status == HttpStatusCode.Unauthorized ||
            (setsResponse.status == HttpStatusCode.OK && setsResponse.bodyAsText().contains("login"))

        assertTrue(isSetsBlocked, "Should not be able to access /sets without session")
    }

    @Test
    fun `GET sets without session should redirect to login`() = testApplication {
        application {
            testModule()
        }

        val response = client.get("/sets")

        // Either we get redirect status or client follows redirect to login page
        val isRedirectedToLogin = response.status == HttpStatusCode.Found ||
            response.status == HttpStatusCode.SeeOther ||
            response.status == HttpStatusCode.Unauthorized ||
            (response.status == HttpStatusCode.OK && response.bodyAsText().contains("login"))

        assertTrue(
            isRedirectedToLogin,
            "Should redirect to login when accessing protected route without session, got ${response.status}",
        )
    }

    @Test
    fun `Session should persist across multiple requests`() = testApplication {
        application {
            testModule()
        }

        // Register and login
        val testUsername = "persist_${System.currentTimeMillis()}"
        val testPassword = "password123"

        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", testPassword)
                append("confirm_password", testPassword)
            },
        )

        // Make multiple requests to protected routes
        val response1 = client.get("/sets")
        val response2 = client.get("/sets")
        val response3 = client.get("/sets")

        assertEquals(HttpStatusCode.OK, response1.status, "First request should succeed")
        assertEquals(HttpStatusCode.OK, response2.status, "Second request should succeed")
        assertEquals(HttpStatusCode.OK, response3.status, "Third request should succeed")
    }
}
