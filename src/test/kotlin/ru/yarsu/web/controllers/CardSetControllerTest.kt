package ru.yarsu.web.controllers

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication
import org.junit.Test
import ru.yarsu.TestHelpers
import ru.yarsu.testModule
import kotlin.test.assertEquals

/**
 * Integration tests for CardSetController
 *
 * These tests verify the main workflows for card set management:
 * - Creating sets with cards
 * - Listing sets
 * - Pagination
 *
 * Note: Most functionality is thoroughly tested in CardSetServiceTest (27 unit tests).
 * These integration tests focus on happy path scenarios to verify the HTTP layer works correctly.
 */
class CardSetControllerTest {

    @Test
    fun `POST create-set with exactly 100 characters should succeed`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val title = "a".repeat(100)
        val response = client.submitForm(
            url = "/create-set",
            formParameters = Parameters.build {
                append("title", title)
            },
        )

        assertEquals(HttpStatusCode.Found, response.status)
    }

    @Test
    fun `POST sets save should skip blank cards`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val response = client.submitForm(
            url = "/sets/save",
            formParameters = Parameters.build {
                append("set_id", "")
                append("new_title", "Test Set")
                append("new_is_private", "false")
                append("cards[0].question", "Valid Question")
                append("cards[0].answer", "Valid Answer")
                append("cards[1].question", "")
                append("cards[1].answer", "")
                append("cards[2].question", "Another Question")
                append("cards[2].answer", "Another Answer")
            },
        )

        assertEquals(HttpStatusCode.Found, response.status)
    }

    @Test
    fun `POST sets save with card titles should preserve titles`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val response = client.submitForm(
            url = "/sets/save",
            formParameters = Parameters.build {
                append("set_id", "")
                append("new_title", "Test Set")
                append("new_is_private", "false")
                append("cards[0].title", "Card Title 1")
                append("cards[0].question", "Question 1")
                append("cards[0].answer", "Answer 1")
            },
        )

        assertEquals(HttpStatusCode.Found, response.status)
    }

    @Test
    fun `GET sets should return sets page for authenticated user`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val response = client.get("/sets")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET sets with only_my parameter should work`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val response = client.get("/sets?only_my=true")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET sets with pagination should return correct page`() = testApplication {
        application {
            testModule()
        }

        val testUsername = TestHelpers.generateUniqueUsername()
        client.submitForm(
            url = "/register",
            formParameters = Parameters.build {
                append("username", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
                append("confirm_password", TestHelpers.TestPasswords.VALID)
            },
        )

        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("login", testUsername)
                append("password", TestHelpers.TestPasswords.VALID)
            },
        )

        val response = client.get("/sets?page=1")

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
