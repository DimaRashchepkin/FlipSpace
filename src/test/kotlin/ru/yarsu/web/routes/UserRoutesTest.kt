package ru.yarsu.web.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Before
import org.junit.Test
import ru.yarsu.TestDatabaseConnection
import ru.yarsu.testModule
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun cleanup() {
        // Clean up users before each test
        val connection = TestDatabaseConnection.getConnection()
        connection.createStatement().execute("DELETE FROM users")
    }

    @Test
    fun `GET users should return all users`() = testApplication {
        application { testModule() }

        // Create some users
        client.post("/users/create?login=user1&password=password1")
        client.post("/users/create?login=user2&password=password2")

        // Act
        val response = client.get("/users")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Users retrieved successfully", responseJson["message"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertTrue(dataArray.size >= 2, "Should have at least 2 users")
    }

    @Test
    fun `GET users should return empty array when no users exist`() = testApplication {
        application { testModule() }

        // Act
        val response = client.get("/users")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(0, dataArray.size, "Should have 0 users")
    }

    @Test
    fun `POST users create should create new user with provided parameters`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/users/create?login=testuser&password=testpass123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val userId = responseJson["userId"]?.jsonPrimitive?.content
        assertNotNull(userId, "User ID should be returned")
        assertTrue(userId.isNotEmpty())

        val login = responseJson["login"]?.jsonPrimitive?.content
        assertEquals("testuser", login)
    }

    @Test
    fun `POST users create should use default values when parameters are missing`() = testApplication {
        application { testModule() }

        // Act - no query parameters
        val response = client.post("/users/create")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val userId = responseJson["userId"]?.jsonPrimitive?.content
        assertNotNull(userId)

        // Default login should be used
        val login = responseJson["login"]?.jsonPrimitive?.content
        assertEquals("default_user", login)
    }

    @Test
    fun `POST users create should generate unique IDs for multiple users`() = testApplication {
        application { testModule() }

        // Act
        val response1 = client.post("/users/create?login=user1&password=pass1")
        val response2 = client.post("/users/create?login=user2&password=pass2")

        // Assert
        val json1 = json.parseToJsonElement(response1.bodyAsText()).jsonObject
        val json2 = json.parseToJsonElement(response2.bodyAsText()).jsonObject

        val userId1 = json1["userId"]?.jsonPrimitive?.content
        val userId2 = json2["userId"]?.jsonPrimitive?.content

        assertNotNull(userId1)
        assertNotNull(userId2)
        assertTrue(userId1 != userId2, "User IDs should be unique")
    }

    @Test
    fun `GET users by-login should return specific user`() = testApplication {
        application { testModule() }

        // Create a user
        val createResponse = client.post("/users/create?login=findme&password=mypassword")
        val createJson = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val userId = createJson["userId"]?.jsonPrimitive?.content
        assertNotNull(userId)

        // Act
        val response = client.get("/users/by-login/findme")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("User found", responseJson["message"]?.jsonPrimitive?.content)

        val data = responseJson["data"]?.jsonObject
        assertNotNull(data)

        assertEquals(userId, data["id"]?.jsonPrimitive?.content)
        assertEquals("findme", data["login"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET users by-login should return not_found for non-existent user`() = testApplication {
        application { testModule() }

        // Act
        val response = client.get("/users/by-login/nonexistent")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("not_found", responseJson["status"]?.jsonPrimitive?.content)
        assertTrue(
            responseJson["error"]?.jsonPrimitive?.content?.contains("not found") == true,
            "Error message should indicate user was not found",
        )
    }

    @Test
    fun `POST users create with special characters in login should work`() = testApplication {
        application { testModule() }

        // Act
        val specialLogin = "user_test-123"
        val response = client.post("/users/create?login=$specialLogin&password=password123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals(specialLogin, responseJson["login"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET users should return all created users`() = testApplication {
        application { testModule() }

        // Create multiple users
        client.post("/users/create?login=user1&password=pass1")
        client.post("/users/create?login=user2&password=pass2")
        client.post("/users/create?login=user3&password=pass3")

        // Act
        val response = client.get("/users")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Verify all logins are present
        val logins = dataArray.map { it.jsonObject["login"]?.jsonPrimitive?.content }.toSet()
        assertTrue(logins.contains("user1"))
        assertTrue(logins.contains("user2"))
        assertTrue(logins.contains("user3"))
    }

    @Test
    fun `GET users by-login should be case-sensitive`() = testApplication {
        application { testModule() }

        // Create a user with lowercase login
        client.post("/users/create?login=testuser&password=password123")

        // Act - try to find with different case
        val response = client.get("/users/by-login/TestUser")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        // Should not find the user (case-sensitive)
        assertEquals("not_found", responseJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST users create should handle multiple users with different passwords`() = testApplication {
        application { testModule() }

        // Act - create users with different passwords
        client.post("/users/create?login=user1&password=short")
        client.post("/users/create?login=user2&password=verylongpassword123456789")
        client.post("/users/create?login=user3&password=medium123")

        // Assert - all should be created successfully
        val response = client.get("/users")
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)
    }

    @Test
    fun `GET users should not expose user passwords`() = testApplication {
        application { testModule() }

        // Create a user
        client.post("/users/create?login=secureuser&password=secretpassword")

        // Act
        val response = client.get("/users")

        // Assert
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)

        // Verify password is not in the response
        dataArray.forEach { user ->
            val userObj = user.jsonObject
            assertTrue(userObj["password"] == null, "Password should not be exposed in API response")
        }
    }

    @Test
    fun `GET users by-login should not expose user password`() = testApplication {
        application { testModule() }

        // Create a user
        client.post("/users/create?login=secureuser&password=secretpassword")

        // Act
        val response = client.get("/users/by-login/secureuser")

        // Assert
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val data = responseJson["data"]?.jsonObject

        assertNotNull(data)
        assertTrue(data["password"] == null, "Password should not be exposed in API response")
    }

    @Test
    fun `POST users create with no login parameter should use default`() = testApplication {
        application { testModule() }

        // Act - omit login parameter entirely
        val response = client.post("/users/create?password=password123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("default_user", responseJson["login"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST users create with empty password should use default`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/users/create?login=testuser&password=")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET users by-login with special characters should work`() = testApplication {
        application { testModule() }

        // Create user with special characters
        val specialLogin = "user-test_123"
        client.post("/users/create?login=$specialLogin&password=password")

        // Act
        val response = client.get("/users/by-login/$specialLogin")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val data = responseJson["data"]?.jsonObject
        assertNotNull(data)
        assertEquals(specialLogin, data["login"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST users create should handle many users efficiently`() = testApplication {
        application { testModule() }

        // Act - create many users
        repeat(20) { index ->
            client.post("/users/create?login=user$index&password=password$index")
        }

        // Assert - all should be retrievable
        val response = client.get("/users")
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(20, dataArray.size)
    }

    @Test
    fun `GET users should return valid user objects`() = testApplication {
        application { testModule() }

        // Create a user
        client.post("/users/create?login=validuser&password=validpass")

        // Act
        val response = client.get("/users")

        // Assert
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertTrue(dataArray.isNotEmpty())

        // Check structure of user object
        val firstUser = dataArray[0].jsonObject
        assertNotNull(firstUser["id"]?.jsonPrimitive?.content, "User should have id")
        assertNotNull(firstUser["login"]?.jsonPrimitive?.content, "User should have login")
    }
}
