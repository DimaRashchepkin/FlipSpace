package ru.yarsu.services

import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.yarsu.TestDatabaseConnection
import ru.yarsu.db.Card
import ru.yarsu.db.CardCreateRequest
import ru.yarsu.db.CardDatabaseService
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardDatabaseServiceTest {

    private lateinit var connection: Connection
    private lateinit var cardDatabaseService: CardDatabaseService

    @Before
    fun setup() {
        connection = TestDatabaseConnection.getConnection()
        cardDatabaseService = CardDatabaseService(connection)

        // Clean up cards table before each test
        connection.createStatement().execute("DELETE FROM cards")
    }

    @After
    fun tearDown() {
        // Clean up after tests
        connection.createStatement().execute("DELETE FROM cards")
    }

    @Test
    fun `createCard should create card and return ID`() {
        // Arrange
        val cardRequest = CardCreateRequest(
            authorId = "author-123",
            content = "Test card content",
            priority = 5,
        )

        // Act
        val cardId = cardDatabaseService.createCard(cardRequest)

        // Assert
        assertNotNull(cardId, "Card ID should not be null")
        assertTrue(cardId.isNotEmpty(), "Card ID should not be empty")

        // Verify card was actually created
        val createdCard = cardDatabaseService.getCardById(cardId)
        assertNotNull(createdCard)
        assertEquals("author-123", createdCard.authorId)
        assertEquals("Test card content", createdCard.content)
        assertEquals(5, createdCard.priority)
    }

    @Test
    fun `createCard should generate unique IDs for multiple cards`() {
        // Arrange
        val cardRequest1 = CardCreateRequest("author-1", "Card 1", 1)
        val cardRequest2 = CardCreateRequest("author-2", "Card 2", 2)

        // Act
        val cardId1 = cardDatabaseService.createCard(cardRequest1)
        val cardId2 = cardDatabaseService.createCard(cardRequest2)

        // Assert
        assertTrue(cardId1 != cardId2, "Card IDs should be unique")
    }

    @Test
    fun `getCardById should return card when it exists`() {
        // Arrange
        val cardRequest = CardCreateRequest("author-123", "Find me!", 3)
        val cardId = cardDatabaseService.createCard(cardRequest)

        // Act
        val card = cardDatabaseService.getCardById(cardId)

        // Assert
        assertNotNull(card, "Card should be found")
        assertEquals(cardId, card.id)
        assertEquals("author-123", card.authorId)
        assertEquals("Find me!", card.content)
        assertEquals(3, card.priority)
    }

    @Test
    fun `getCardById should return null when card does not exist`() {
        // Act
        val card = cardDatabaseService.getCardById("non-existent-id")

        // Assert
        assertNull(card, "Should return null for non-existent card")
    }

    @Test
    fun `getCardsByAuthor should return all cards by specific author`() {
        // Arrange
        val authorId = "author-123"
        cardDatabaseService.createCard(CardCreateRequest(authorId, "Card 1", 5))
        cardDatabaseService.createCard(CardCreateRequest(authorId, "Card 2", 3))
        cardDatabaseService.createCard(CardCreateRequest("other-author", "Card 3", 1))

        // Act
        val cards = cardDatabaseService.getCardsByAuthor(authorId)

        // Assert
        assertEquals(2, cards.size, "Should return 2 cards for author-123")
        assertTrue(cards.all { it.authorId == authorId }, "All cards should belong to the author")
    }

    @Test
    fun `getCardsByAuthor should return cards sorted by priority descending`() {
        // Arrange
        val authorId = "author-123"
        cardDatabaseService.createCard(CardCreateRequest(authorId, "Low priority", 1))
        cardDatabaseService.createCard(CardCreateRequest(authorId, "High priority", 10))
        cardDatabaseService.createCard(CardCreateRequest(authorId, "Medium priority", 5))

        // Act
        val cards = cardDatabaseService.getCardsByAuthor(authorId)

        // Assert
        assertEquals(3, cards.size)
        assertEquals("High priority", cards[0].content)
        assertEquals("Medium priority", cards[1].content)
        assertEquals("Low priority", cards[2].content)
    }

    @Test
    fun `getCardsByAuthor should return empty list when author has no cards`() {
        // Arrange
        cardDatabaseService.createCard(CardCreateRequest("other-author", "Card 1", 1))

        // Act
        val cards = cardDatabaseService.getCardsByAuthor("non-existent-author")

        // Assert
        assertTrue(cards.isEmpty(), "Should return empty list for author with no cards")
    }

    @Test
    fun `getAllCards should return all cards sorted by priority`() {
        // Arrange
        cardDatabaseService.createCard(CardCreateRequest("author-1", "Card 1", 5))
        cardDatabaseService.createCard(CardCreateRequest("author-2", "Card 2", 10))
        cardDatabaseService.createCard(CardCreateRequest("author-3", "Card 3", 2))

        // Act
        val cards = cardDatabaseService.getAllCards()

        // Assert
        assertEquals(3, cards.size)
        assertEquals(10, cards[0].priority, "First card should have highest priority")
        assertEquals(5, cards[1].priority)
        assertEquals(2, cards[2].priority, "Last card should have lowest priority")
    }

    @Test
    fun `getAllCards should return empty list when no cards exist`() {
        // Act
        val cards = cardDatabaseService.getAllCards()

        // Assert
        assertTrue(cards.isEmpty(), "Should return empty list when no cards exist")
    }

    @Test
    fun `updateCard should modify content and priority`() {
        // Arrange
        val cardRequest = CardCreateRequest("author-123", "Original content", 5)
        val cardId = cardDatabaseService.createCard(cardRequest)

        // Act
        val updated = cardDatabaseService.updateCard(cardId, "Updated content", 8)

        // Assert
        assertTrue(updated, "Update should return true")

        val card = cardDatabaseService.getCardById(cardId)
        assertNotNull(card)
        assertEquals("Updated content", card.content)
        assertEquals(8, card.priority)
        // Author should remain unchanged
        assertEquals("author-123", card.authorId)
    }

    @Test
    fun `updateCard should return false when card does not exist`() {
        // Act
        val updated = cardDatabaseService.updateCard("non-existent-id", "Content", 1)

        // Assert
        assertFalse(updated, "Update should return false for non-existent card")
    }

    @Test
    fun `deleteCard should remove card and return true`() {
        // Arrange
        val cardRequest = CardCreateRequest("author-123", "Delete me", 1)
        val cardId = cardDatabaseService.createCard(cardRequest)

        // Act
        val deleted = cardDatabaseService.deleteCard(cardId)

        // Assert
        assertTrue(deleted, "Delete should return true")
        assertNull(cardDatabaseService.getCardById(cardId), "Card should no longer exist")
    }

    @Test
    fun `deleteCard should return false when card does not exist`() {
        // Act
        val deleted = cardDatabaseService.deleteCard("non-existent-id")

        // Assert
        assertFalse(deleted, "Delete should return false for non-existent card")
    }

    @Test
    fun `getRandomCardByPriority should return card when cards exist`() {
        // Arrange
        cardDatabaseService.createCard(CardCreateRequest("author-1", "Card 1", 5))
        cardDatabaseService.createCard(CardCreateRequest("author-2", "Card 2", 10))

        // Act
        val randomCard = cardDatabaseService.getRandomCardByPriority()

        // Assert
        assertNotNull(randomCard, "Should return a card when cards exist")
        assertTrue(
            randomCard.content == "Card 1" || randomCard.content == "Card 2",
            "Should return one of the existing cards",
        )
    }

    @Test
    fun `getRandomCardByPriority should return null when no cards exist`() {
        // Act
        val randomCard = cardDatabaseService.getRandomCardByPriority()

        // Assert
        assertNull(randomCard, "Should return null when no cards exist")
    }

    @Test
    fun `getRandomCardByPriority with higher priority should appear more frequently`() {
        // Arrange - create cards with different priorities
        cardDatabaseService.createCard(CardCreateRequest("author-1", "Low priority", 1))
        cardDatabaseService.createCard(CardCreateRequest("author-2", "High priority", 100))

        // Act - get multiple random cards and count occurrences
        var highPriorityCount = 0
        var lowPriorityCount = 0
        val iterations = 50

        repeat(iterations) {
            val card = cardDatabaseService.getRandomCardByPriority()
            when (card?.content) {
                "High priority" -> highPriorityCount++
                "Low priority" -> lowPriorityCount++
            }
        }

        // Assert - high priority card should appear more often
        // With 1:100 ratio, we expect high priority to dominate
        assertTrue(
            highPriorityCount > lowPriorityCount,
            "Higher priority card should appear more frequently. High: $highPriorityCount, Low: $lowPriorityCount",
        )
    }

    @Test
    fun `createCard with various priority values should work correctly`() {
        // Arrange & Act
        val id1 = cardDatabaseService.createCard(CardCreateRequest("author", "Min priority", 1))
        val id2 = cardDatabaseService.createCard(CardCreateRequest("author", "Max priority", 100))
        val id3 = cardDatabaseService.createCard(CardCreateRequest("author", "Medium priority", 50))

        // Assert
        val card1 = cardDatabaseService.getCardById(id1)
        val card2 = cardDatabaseService.getCardById(id2)
        val card3 = cardDatabaseService.getCardById(id3)

        assertEquals(1, card1?.priority)
        assertEquals(100, card2?.priority)
        assertEquals(50, card3?.priority)
    }

    @Test
    fun `createCard with special characters in content should work`() {
        // Arrange
        val specialContent = "Test with special chars: @#$%^&*()_+-=[]{}|;':\",./<>?"
        val cardRequest = CardCreateRequest("author-123", specialContent, 1)

        // Act
        val cardId = cardDatabaseService.createCard(cardRequest)
        val card = cardDatabaseService.getCardById(cardId)

        // Assert
        assertNotNull(card)
        assertEquals(specialContent, card.content)
    }

    @Test
    fun `createCard with long content should work`() {
        // Arrange
        val longContent = "A".repeat(1000)
        val cardRequest = CardCreateRequest("author-123", longContent, 1)

        // Act
        val cardId = cardDatabaseService.createCard(cardRequest)
        val card = cardDatabaseService.getCardById(cardId)

        // Assert
        assertNotNull(card)
        assertEquals(longContent, card.content)
    }

    @Test
    fun `multiple updates to same card should work correctly`() {
        // Arrange
        val cardId = cardDatabaseService.createCard(CardCreateRequest("author", "Original", 1))

        // Act & Assert - Multiple updates
        cardDatabaseService.updateCard(cardId, "Update 1", 2)
        var card = cardDatabaseService.getCardById(cardId)
        assertEquals("Update 1", card?.content)
        assertEquals(2, card?.priority)

        cardDatabaseService.updateCard(cardId, "Update 2", 3)
        card = cardDatabaseService.getCardById(cardId)
        assertEquals("Update 2", card?.content)
        assertEquals(3, card?.priority)

        cardDatabaseService.updateCard(cardId, "Final Update", 5)
        card = cardDatabaseService.getCardById(cardId)
        assertEquals("Final Update", card?.content)
        assertEquals(5, card?.priority)
    }
}
