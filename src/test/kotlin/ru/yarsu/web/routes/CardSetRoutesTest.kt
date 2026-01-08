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

class CardSetRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun cleanup() {
        // Clean up and setup test data
        val connection = TestDatabaseConnection.getConnection()
        connection.createStatement().execute("DELETE FROM card_sets")
        connection.createStatement().execute("DELETE FROM users")

        // Create test users
        connection.createStatement().execute(
            """
            INSERT INTO users (id, login, password) VALUES
            ('12345', 'test_user', 'password'),
            ('user-123', 'user123', 'password'),
            ('other-user', 'other', 'password'),
            ('user-1', 'user1', 'password'),
            ('user-2', 'user2', 'password'),
            ('user-3', 'user3', 'password'),
            ('power-user', 'power', 'password')
            """.trimIndent(),
        )
    }

    @Test
    fun `GET sets should return card sets for current user`() = testApplication {
        application { testModule() }

        // Create some card sets
        client.post("/api/sets/create?userId=12345&title=Set 1")
        client.post("/api/sets/create?userId=12345&title=Set 2")
        client.post("/api/sets/create?userId=other-user&title=Other Set")

        // Act
        val response = client.get("/api/sets")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        // Should include sets for default user (12345)
        assertTrue(dataArray.size >= 2)
    }

    @Test
    fun `GET sets should return empty array when user has no sets`() = testApplication {
        application { testModule() }

        // Don't create any sets for the default user
        client.post("/api/sets/create?userId=other-user&title=Other User Set")

        // Act
        val response = client.get("/api/sets")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(0, dataArray.size)
    }

    @Test
    fun `POST sets create should create new card set with provided parameters`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/api/sets/create?userId=user-123&title=My Test Set&isPrivate=true")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val setId = responseJson["setId"]?.jsonPrimitive?.content
        assertNotNull(setId, "Set ID should be returned")
        assertTrue(setId.isNotEmpty())

        val title = responseJson["title"]?.jsonPrimitive?.content
        assertEquals("My Test Set", title)
    }

    @Test
    fun `POST sets create should use default values when parameters are missing`() = testApplication {
        application { testModule() }

        // Act - no query parameters
        val response = client.post("/api/sets/create")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val setId = responseJson["setId"]?.jsonPrimitive?.content
        assertNotNull(setId)

        val title = responseJson["title"]?.jsonPrimitive?.content
        assertEquals("New Card Set", title)
    }

    @Test
    fun `POST sets create with isPrivate false should create public set`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/api/sets/create?userId=user-123&title=Public Set&isPrivate=false")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertNotNull(responseJson["setId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET sets by-user should return card sets for specific user`() = testApplication {
        application { testModule() }

        // Create card sets for different users
        client.post("/api/sets/create?userId=user-123&title=User 123 Set 1")
        client.post("/api/sets/create?userId=user-123&title=User 123 Set 2")
        client.post("/api/sets/create?userId=other-user&title=Other Set")

        // Act
        val response = client.get("/api/sets/by-user/user-123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Card sets for user user-123", responseJson["message"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(2, dataArray.size, "Should have 2 sets for user-123")

        // Verify all sets belong to the correct user
        dataArray.forEach { cardSet ->
            val userId = cardSet.jsonObject["userId"]?.jsonPrimitive?.content
            assertEquals("user-123", userId)
        }
    }

    @Test
    fun `GET sets by-user should return empty array for user with no sets`() = testApplication {
        application { testModule() }

        // Create sets for a different user
        client.post("/api/sets/create?userId=other-user&title=Set")

        // Act
        val response = client.get("/api/sets/by-user/non-existent-user")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(0, dataArray.size)
    }

    @Test
    fun `GET sets all should return all card sets`() = testApplication {
        application { testModule() }

        // Create card sets for different users
        client.post("/api/sets/create?userId=user-1&title=Set 1")
        client.post("/api/sets/create?userId=user-2&title=Set 2")
        client.post("/api/sets/create?userId=user-3&title=Set 3")

        // Act
        val response = client.get("/api/sets/all")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("All card sets", responseJson["message"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)
    }

    @Test
    fun `GET sets all should return empty array when no sets exist`() = testApplication {
        application { testModule() }

        // Act
        val response = client.get("/api/sets/all")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(0, dataArray.size)
    }

    @Test
    fun `POST sets create with simple title should work`() = testApplication {
        application { testModule() }

        // Act - use simple alphanumeric title
        val response = client.post("/api/sets/create?userId=user-1&title=Simple Title")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertNotNull(responseJson["setId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET sets should handle multiple sets correctly`() = testApplication {
        application { testModule() }

        // Create multiple sets for the default user
        client.post("/api/sets/create?userId=12345&title=Set Alpha")
        client.post("/api/sets/create?userId=12345&title=Set Beta")
        client.post("/api/sets/create?userId=12345&title=Set Gamma")

        // Act
        val response = client.get("/api/sets")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Verify all sets belong to the default user
        dataArray.forEach { cardSet ->
            val userId = cardSet.jsonObject["userId"]?.jsonPrimitive?.content
            assertEquals("12345", userId)
        }
    }

    @Test
    fun `GET sets should return sets sorted by creation time descending`() = testApplication {
        application { testModule() }

        // Create sets in order
        val response1 = client.post("/api/sets/create?userId=12345&title=First Set")
        val json1 = json.parseToJsonElement(response1.bodyAsText()).jsonObject
        val firstSetId = json1["setId"]?.jsonPrimitive?.content

        // Small delay to ensure different timestamps
        Thread.sleep(10)

        val response2 = client.post("/api/sets/create?userId=12345&title=Second Set")
        val json2 = json.parseToJsonElement(response2.bodyAsText()).jsonObject
        val secondSetId = json2["setId"]?.jsonPrimitive?.content

        Thread.sleep(10)

        val response3 = client.post("/api/sets/create?userId=12345&title=Third Set")
        val json3 = json.parseToJsonElement(response3.bodyAsText()).jsonObject
        val thirdSetId = json3["setId"]?.jsonPrimitive?.content

        // Act
        val response = client.get("/api/sets")

        // Assert
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Most recent should be first (Third Set)
        assertEquals("Third Set", dataArray[0].jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals("Second Set", dataArray[1].jsonObject["title"]?.jsonPrimitive?.content)
        assertEquals("First Set", dataArray[2].jsonObject["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET sets by-user should include both public and private sets for that user`() = testApplication {
        application { testModule() }

        // Create both public and private sets
        client.post("/api/sets/create?userId=user-123&title=Public Set&isPrivate=false")
        client.post("/api/sets/create?userId=user-123&title=Private Set&isPrivate=true")

        // Act
        val response = client.get("/api/sets/by-user/user-123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(2, dataArray.size, "Should return both public and private sets for the user")
    }

    @Test
    fun `POST sets create should generate unique IDs for multiple sets`() = testApplication {
        application { testModule() }

        // Act
        val response1 = client.post("/api/sets/create?userId=user-1&title=Set 1")
        val response2 = client.post("/api/sets/create?userId=user-2&title=Set 2")

        // Assert
        val json1 = json.parseToJsonElement(response1.bodyAsText()).jsonObject
        val json2 = json.parseToJsonElement(response2.bodyAsText()).jsonObject

        val setId1 = json1["setId"]?.jsonPrimitive?.content
        val setId2 = json2["setId"]?.jsonPrimitive?.content

        assertNotNull(setId1)
        assertNotNull(setId2)
        assertTrue(setId1 != setId2, "Set IDs should be unique")
    }

    @Test
    fun `GET sets all should include sets from all users`() = testApplication {
        application { testModule() }

        // Create sets for multiple users
        client.post("/api/sets/create?userId=user-1&title=User 1 Set")
        client.post("/api/sets/create?userId=user-2&title=User 2 Set")
        client.post("/api/sets/create?userId=user-3&title=User 3 Set")

        // Act
        val response = client.get("/api/sets/all")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Verify all three users are present
        val userIds = dataArray.map { it.jsonObject["userId"]?.jsonPrimitive?.content }.toSet()
        assertTrue(userIds.contains("user-1"))
        assertTrue(userIds.contains("user-2"))
        assertTrue(userIds.contains("user-3"))
    }

    @Test
    fun `POST sets create with long title should work`() = testApplication {
        application { testModule() }

        // Act
        val longTitle = "A".repeat(200)
        val response = client.post("/api/sets/create?userId=user-1&title=$longTitle")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET sets by-user should handle user with many sets`() = testApplication {
        application { testModule() }

        // Create many sets for one user
        repeat(10) { index ->
            client.post("/api/sets/create?userId=power-user&title=Set $index")
        }

        // Act
        val response = client.get("/api/sets/by-user/power-user")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(10, dataArray.size)
    }
}
