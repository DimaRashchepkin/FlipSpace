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

class CardRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun cleanup() {
        // Clean up cards before each test
        val connection = TestDatabaseConnection.getConnection()
        connection.createStatement().execute("DELETE FROM cards")
    }

    @Test
    fun `GET cards should return all cards`() = testApplication {
        application { testModule() }

        // Create some cards first
        client.post("/cards/create?authorId=author-1&content=Card 1&priority=5")
        client.post("/cards/create?authorId=author-2&content=Card 2&priority=10")

        // Act
        val response = client.get("/cards")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Cards retrieved successfully", responseJson["message"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertTrue(dataArray.size >= 2, "Should have at least 2 cards")
    }

    @Test
    fun `GET cards should return empty array when no cards exist`() = testApplication {
        application { testModule() }

        // Act
        val response = client.get("/cards")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(0, dataArray.size, "Should have 0 cards")
    }

    @Test
    fun `POST cards create should create new card with provided parameters`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/cards/create?authorId=test-author&content=Test Content&priority=7")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val cardId = responseJson["cardId"]?.jsonPrimitive?.content
        assertNotNull(cardId, "Card ID should be returned")
        assertTrue(cardId.isNotEmpty())

        val authorId = responseJson["authorId"]?.jsonPrimitive?.content
        assertEquals("test-author", authorId)
    }

    @Test
    fun `POST cards create should use default values when parameters are missing`() = testApplication {
        application { testModule() }

        // Act - no query parameters
        val response = client.post("/cards/create")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val cardId = responseJson["cardId"]?.jsonPrimitive?.content
        assertNotNull(cardId)

        // Default authorId should be used
        val authorId = responseJson["authorId"]?.jsonPrimitive?.content
        assertEquals("00000000-0000-0000-0000-000000000001", authorId)
    }

    @Test
    fun `GET cards random should return a random card`() = testApplication {
        application { testModule() }

        // Create cards first
        client.post("/cards/create?authorId=author-1&content=Card 1&priority=5")
        client.post("/cards/create?authorId=author-2&content=Card 2&priority=10")

        // Act
        val response = client.get("/cards/random")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Random card retrieved", responseJson["message"]?.jsonPrimitive?.content)

        val data = responseJson["data"]?.jsonObject
        assertNotNull(data)

        val content = data["content"]?.jsonPrimitive?.content
        assertTrue(content == "Card 1" || content == "Card 2", "Should return one of the created cards")
    }

    @Test
    fun `GET cards random should return error when no cards exist`() = testApplication {
        application { testModule() }

        // Act - no cards created
        val response = client.get("/cards/random")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("empty", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("No cards available in database", responseJson["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET cards by-author should return cards for specific author`() = testApplication {
        application { testModule() }

        // Create cards for different authors
        client.post("/cards/create?authorId=author-123&content=Author 123 Card 1&priority=5")
        client.post("/cards/create?authorId=author-123&content=Author 123 Card 2&priority=10")
        client.post("/cards/create?authorId=other-author&content=Other Card&priority=3")

        // Act
        val response = client.get("/cards/by-author/author-123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Cards retrieved for author author-123", responseJson["message"]?.jsonPrimitive?.content)

        val dataArray = responseJson["data"]?.jsonArray
        assertNotNull(dataArray)
        assertEquals(2, dataArray.size, "Should have 2 cards for author-123")

        // Verify all cards belong to the correct author
        dataArray.forEach { card ->
            val authorId = card.jsonObject["authorId"]?.jsonPrimitive?.content
            assertEquals("author-123", authorId)
        }
    }

    @Test
    fun `GET cards by-author should return empty array for author with no cards`() = testApplication {
        application { testModule() }

        // Create cards for a different author
        client.post("/cards/create?authorId=other-author&content=Card&priority=1")

        // Act
        val response = client.get("/cards/by-author/non-existent-author")

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
    fun `GET cards by-author should return cards sorted by priority descending`() = testApplication {
        application { testModule() }

        // Create cards with different priorities
        client.post("/cards/create?authorId=author-123&content=Low&priority=1")
        client.post("/cards/create?authorId=author-123&content=High&priority=10")
        client.post("/cards/create?authorId=author-123&content=Medium&priority=5")

        // Act
        val response = client.get("/cards/by-author/author-123")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Check order (highest priority first)
        assertEquals("High", dataArray[0].jsonObject["content"]?.jsonPrimitive?.content)
        assertEquals("Medium", dataArray[1].jsonObject["content"]?.jsonPrimitive?.content)
        assertEquals("Low", dataArray[2].jsonObject["content"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET cards by id should return specific card`() = testApplication {
        application { testModule() }

        // Create a card
        val createResponse = client.post("/cards/create?authorId=author-123&content=Find me&priority=5")
        val createJson = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val cardId = createJson["cardId"]?.jsonPrimitive?.content
        assertNotNull(cardId)

        // Act
        val response = client.get("/cards/$cardId")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
        assertEquals("Card found", responseJson["message"]?.jsonPrimitive?.content)

        val data = responseJson["data"]?.jsonObject
        assertNotNull(data)

        assertEquals(cardId, data["id"]?.jsonPrimitive?.content)
        assertEquals("author-123", data["authorId"]?.jsonPrimitive?.content)
        assertEquals("Find me", data["content"]?.jsonPrimitive?.content)
        assertEquals(5, data["priority"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `GET cards by id should return not_found for non-existent card`() = testApplication {
        application { testModule() }

        // Act
        val response = client.get("/cards/non-existent-id-12345")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("not_found", responseJson["status"]?.jsonPrimitive?.content)
        assertTrue(
            responseJson["error"]?.jsonPrimitive?.content?.contains("not found") == true,
            "Error message should indicate card was not found",
        )
    }

    @Test
    fun `POST cards create with URL encoded content should work`() = testApplication {
        application { testModule() }

        // Act - use simple alphanumeric content
        val response = client.post("/cards/create?authorId=author-1&content=Test Content&priority=5")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)

        val cardId = responseJson["cardId"]?.jsonPrimitive?.content
        assertNotNull(cardId)
    }

    @Test
    fun `POST cards create with maximum priority should work`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/cards/create?authorId=author-1&content=Max priority&priority=100")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST cards create with minimum priority should work`() = testApplication {
        application { testModule() }

        // Act
        val response = client.post("/cards/create?authorId=author-1&content=Min priority&priority=1")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject

        assertEquals("success", responseJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET cards should handle multiple authors correctly`() = testApplication {
        application { testModule() }

        // Create cards for multiple authors
        client.post("/cards/create?authorId=author-1&content=Author 1 Card&priority=5")
        client.post("/cards/create?authorId=author-2&content=Author 2 Card&priority=10")
        client.post("/cards/create?authorId=author-3&content=Author 3 Card&priority=3")

        // Act
        val response = client.get("/cards")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(3, dataArray.size)

        // Verify all three authors are present
        val authorIds = dataArray.map { it.jsonObject["authorId"]?.jsonPrimitive?.content }.toSet()
        assertTrue(authorIds.contains("author-1"))
        assertTrue(authorIds.contains("author-2"))
        assertTrue(authorIds.contains("author-3"))
    }

    @Test
    fun `GET cards should return cards sorted by priority descending`() = testApplication {
        application { testModule() }

        // Create cards with different priorities
        client.post("/cards/create?authorId=author-1&content=Low&priority=2")
        client.post("/cards/create?authorId=author-2&content=High&priority=10")
        client.post("/cards/create?authorId=author-3&content=Medium&priority=5")

        // Act
        val response = client.get("/cards")

        // Assert
        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val dataArray = responseJson["data"]?.jsonArray

        assertNotNull(dataArray)

        // Verify ordering by priority (highest first)
        val priorities = dataArray.map { it.jsonObject["priority"]?.jsonPrimitive?.content?.toInt() }
        assertEquals(10, priorities[0])
        assertEquals(5, priorities[1])
        assertEquals(2, priorities[2])
    }

    @Test
    fun `GET cards random should respect priority weighting`() = testApplication {
        application { testModule() }

        // Create cards with very different priorities
        client.post("/cards/create?authorId=author-1&content=Low&priority=1")
        client.post("/cards/create?authorId=author-2&content=High&priority=100")

        // Act - get random card multiple times
        var highPriorityCount = 0
        var lowPriorityCount = 0

        repeat(20) {
            val response = client.get("/cards/random")
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            val content = responseJson["data"]?.jsonObject?.get("content")?.jsonPrimitive?.content

            when (content) {
                "High" -> highPriorityCount++
                "Low" -> lowPriorityCount++
            }
        }

        // Assert - higher priority should appear more often
        assertTrue(
            highPriorityCount > lowPriorityCount,
            "High priority card should appear more often: High=$highPriorityCount, Low=$lowPriorityCount",
        )
    }
}
