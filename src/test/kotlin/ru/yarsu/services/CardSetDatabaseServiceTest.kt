package ru.yarsu.services

import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.yarsu.TestDatabaseConnection
import ru.yarsu.db.CardSetDatabaseService
import ru.yarsu.db.SetCard
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardSetDatabaseServiceTest {

    private lateinit var connection: Connection
    private lateinit var cardSetService: CardSetDatabaseService

    @Before
    fun setup() {
        connection = TestDatabaseConnection.getConnection()
        cardSetService = CardSetDatabaseService(connection)

        // Clean up before each test
        connection.createStatement().execute("DELETE FROM cards WHERE set_id IN (SELECT id FROM card_sets)")
        connection.createStatement().execute("DELETE FROM card_sets")
        connection.createStatement().execute("DELETE FROM users")

        // Create test users
        connection.createStatement().execute(
            """
            INSERT INTO users (id, login, password) VALUES
            ('user-1', 'user1', 'pass1'),
            ('user-2', 'user2', 'pass2'),
            ('user-3', 'user3', 'pass3')
            """.trimIndent(),
        )
    }

    @After
    fun tearDown() {
        connection.createStatement().execute("DELETE FROM cards WHERE set_id IN (SELECT id FROM card_sets)")
        connection.createStatement().execute("DELETE FROM card_sets")
        connection.createStatement().execute("DELETE FROM users")
    }

    @Test
    fun `createCardSet should create card set and return ID`() {
        // Arrange
        val userId = "user-1"
        val title = "Test Card Set"
        val isPrivate = false

        // Act
        val setId = cardSetService.createCardSet(userId, title, isPrivate)

        // Assert
        assertNotNull(setId, "Set ID should not be null")
        assertTrue(setId.isNotEmpty(), "Set ID should not be empty")

        // Verify set was actually created
        val createdSet = cardSetService.getCardSetById(setId)
        assertNotNull(createdSet)
        assertEquals(userId, createdSet.userId)
        assertEquals(title, createdSet.title)
        assertEquals(isPrivate, createdSet.isPrivate)
    }

    @Test
    fun `createCardSet should generate unique IDs for multiple sets`() {
        // Act
        val setId1 = cardSetService.createCardSet("user-1", "Set 1", false)
        val setId2 = cardSetService.createCardSet("user-1", "Set 2", false)

        // Assert
        assertTrue(setId1 != setId2, "Set IDs should be unique")
    }

    @Test
    fun `createCardSet with private flag should create private set`() {
        // Act
        val setId = cardSetService.createCardSet("user-1", "Private Set", true)
        val cardSet = cardSetService.getCardSetById(setId)

        // Assert
        assertNotNull(cardSet)
        assertTrue(cardSet.isPrivate, "Set should be private")
    }

    @Test
    fun `getCardSetById should return null for non-existent set`() {
        // Act
        val cardSet = cardSetService.getCardSetById("non-existent-id")

        // Assert
        assertNull(cardSet, "Should return null for non-existent set")
    }

    @Test
    fun `getCardSetsByUser should return all sets for user`() {
        // Arrange
        cardSetService.createCardSet("user-1", "Set 1", false)
        cardSetService.createCardSet("user-1", "Set 2", true)
        cardSetService.createCardSet("user-2", "Other Set", false)

        // Act
        val sets = cardSetService.getCardSetsByUser("user-1")

        // Assert
        assertEquals(2, sets.size, "Should return 2 sets for user-1")
        assertTrue(sets.all { it.userId == "user-1" }, "All sets should belong to user-1")
    }

    @Test
    fun `getCardSetsByUser should return empty list for user with no sets`() {
        // Arrange
        cardSetService.createCardSet("user-1", "Set 1", false)

        // Act
        val sets = cardSetService.getCardSetsByUser("user-2")

        // Assert
        assertTrue(sets.isEmpty(), "Should return empty list for user with no sets")
    }

    @Test
    fun `getCardSetsByUser should return sets sorted by creation time descending`() {
        // Arrange
        val setId1 = cardSetService.createCardSet("user-1", "First Set", false)
        Thread.sleep(10)
        val setId2 = cardSetService.createCardSet("user-1", "Second Set", false)
        Thread.sleep(10)
        val setId3 = cardSetService.createCardSet("user-1", "Third Set", false)

        // Act
        val sets = cardSetService.getCardSetsByUser("user-1")

        // Assert
        assertEquals(3, sets.size)
        assertEquals(setId3, sets[0].id, "Most recent set should be first")
        assertEquals(setId2, sets[1].id)
        assertEquals(setId1, sets[2].id, "Oldest set should be last")
    }

    @Test
    fun `getAllCardSets should return all sets`() {
        // Arrange
        cardSetService.createCardSet("user-1", "Set 1", false)
        cardSetService.createCardSet("user-2", "Set 2", true)
        cardSetService.createCardSet("user-3", "Set 3", false)

        // Act
        val sets = cardSetService.getAllCardSets()

        // Assert
        assertEquals(3, sets.size, "Should return all 3 sets")
    }

    @Test
    fun `getAllCardSets should return empty list when no sets exist`() {
        // Act
        val sets = cardSetService.getAllCardSets()

        // Assert
        assertTrue(sets.isEmpty(), "Should return empty list when no sets exist")
    }

    @Test
    fun `getAllCardSetsVisibleToUser with userId should return public sets and user's private sets`() {
        // Arrange
        cardSetService.createCardSet("user-1", "User 1 Private", true)
        cardSetService.createCardSet("user-1", "User 1 Public", false)
        cardSetService.createCardSet("user-2", "User 2 Private", true)
        cardSetService.createCardSet("user-2", "User 2 Public", false)

        // Act
        val sets = cardSetService.getAllCardSetsVisibleToUser("user-1")

        // Assert
        assertEquals(3, sets.size, "Should return 3 sets (user-1's private + user-1's public + user-2's public)")

        val titles = sets.map { it.title }.toSet()
        assertTrue(titles.contains("User 1 Private"))
        assertTrue(titles.contains("User 1 Public"))
        assertTrue(titles.contains("User 2 Public"))
        assertFalse(titles.contains("User 2 Private"), "Should not include other user's private sets")
    }

    @Test
    fun `getAllCardSetsVisibleToUser with null userId should return only public sets`() {
        // Arrange
        cardSetService.createCardSet("user-1", "User 1 Private", true)
        cardSetService.createCardSet("user-1", "User 1 Public", false)
        cardSetService.createCardSet("user-2", "User 2 Private", true)
        cardSetService.createCardSet("user-2", "User 2 Public", false)

        // Act
        val sets = cardSetService.getAllCardSetsVisibleToUser(null)

        // Assert
        assertEquals(2, sets.size, "Should return only 2 public sets")

        val titles = sets.map { it.title }.toSet()
        assertTrue(titles.contains("User 1 Public"))
        assertTrue(titles.contains("User 2 Public"))
        assertFalse(titles.contains("User 1 Private"))
        assertFalse(titles.contains("User 2 Private"))
    }

    @Test
    fun `updateCardSet should update title and isPrivate`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Original Title", false)

        // Act
        cardSetService.updateCardSet(setId, "Updated Title", true)

        // Assert
        val updatedSet = cardSetService.getCardSetById(setId)
        assertNotNull(updatedSet)
        assertEquals("Updated Title", updatedSet.title)
        assertTrue(updatedSet.isPrivate)
        assertEquals("user-1", updatedSet.userId, "User ID should not change")
    }

    @Test
    fun `updateCardSet should change private to public`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", true)

        // Act
        cardSetService.updateCardSet(setId, "Test Set", false)

        // Assert
        val updatedSet = cardSetService.getCardSetById(setId)
        assertNotNull(updatedSet)
        assertFalse(updatedSet.isPrivate)
    }

    @Test
    fun `deleteCardSet should remove card set`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "To Delete", false)

        // Act
        cardSetService.deleteCardSet(setId)

        // Assert
        val deletedSet = cardSetService.getCardSetById(setId)
        assertNull(deletedSet, "Card set should be deleted")
    }

    @Test
    fun `saveCardsForSet should save new cards to empty set`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, "Card 1", "Front 1", "Back 1"),
            SetCard("card-2", setId, "Card 2", "Front 2", "Back 2"),
        )

        // Act
        cardSetService.saveCardsForSet(setId, cards)

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertEquals(2, savedCards.size)
        assertEquals("Front 1", savedCards[0].frontText)
        assertEquals("Back 1", savedCards[0].backText)
    }

    @Test
    fun `saveCardsForSet should replace existing cards`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val initialCards = listOf(
            SetCard("card-1", setId, "Old Card", "Old Front", "Old Back"),
        )
        cardSetService.saveCardsForSet(setId, initialCards)

        val newCards = listOf(
            SetCard("card-2", setId, "New Card 1", "New Front 1", "New Back 1"),
            SetCard("card-3", setId, "New Card 2", "New Front 2", "New Back 2"),
        )

        // Act
        cardSetService.saveCardsForSet(setId, newCards)

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertEquals(2, savedCards.size, "Should have 2 new cards")
        assertFalse(savedCards.any { it.id == "card-1" }, "Old card should be removed")
        assertTrue(savedCards.any { it.id == "card-2" })
        assertTrue(savedCards.any { it.id == "card-3" })
    }

    @Test
    fun `saveCardsForSet with empty list should delete all cards`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, "Card 1", "Front 1", "Back 1"),
        )
        cardSetService.saveCardsForSet(setId, cards)

        // Act
        cardSetService.saveCardsForSet(setId, emptyList())

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertTrue(savedCards.isEmpty(), "All cards should be deleted")
    }

    @Test
    fun `getCardsBySetId should return empty list for set with no cards`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Empty Set", false)

        // Act
        val cards = cardSetService.getCardsBySetId(setId)

        // Assert
        assertTrue(cards.isEmpty(), "Should return empty list for set with no cards")
    }

    @Test
    fun `getCardsBySetId should return cards sorted by creation time`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, null, "First", "Back 1"),
            SetCard("card-2", setId, null, "Second", "Back 2"),
            SetCard("card-3", setId, null, "Third", "Back 3"),
        )
        cardSetService.saveCardsForSet(setId, cards)

        // Act
        val savedCards = cardSetService.getCardsBySetId(setId)

        // Assert
        assertEquals(3, savedCards.size)
        assertEquals("card-1", savedCards[0].id)
        assertEquals("card-2", savedCards[1].id)
        assertEquals("card-3", savedCards[2].id)
    }

    @Test
    fun `deleteCardSet should cascade delete cards`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, "Card 1", "Front 1", "Back 1"),
            SetCard("card-2", setId, "Card 2", "Front 2", "Back 2"),
        )
        cardSetService.saveCardsForSet(setId, cards)

        // Act
        cardSetService.deleteCardSet(setId)

        // Assert
        val remainingCards = cardSetService.getCardsBySetId(setId)
        assertTrue(remainingCards.isEmpty(), "Cards should be cascade deleted with set")
    }

    @Test
    fun `createCardSet with long title should work`() {
        // Arrange
        val longTitle = "A".repeat(200)

        // Act
        val setId = cardSetService.createCardSet("user-1", longTitle, false)
        val cardSet = cardSetService.getCardSetById(setId)

        // Assert
        assertNotNull(cardSet)
        assertEquals(longTitle, cardSet.title)
    }

    @Test
    fun `createCardSet with Unicode title should work`() {
        // Arrange
        val unicodeTitle = "Карточки 中文 日本語 🎓"

        // Act
        val setId = cardSetService.createCardSet("user-1", unicodeTitle, false)
        val cardSet = cardSetService.getCardSetById(setId)

        // Assert
        assertNotNull(cardSet)
        assertEquals(unicodeTitle, cardSet.title)
    }

    @Test
    fun `saveCardsForSet with card title null should work`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, null, "Front", "Back"),
        )

        // Act
        cardSetService.saveCardsForSet(setId, cards)

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertEquals(1, savedCards.size)
        assertNull(savedCards[0].title)
    }

    @Test
    fun `saveCardsForSet with Unicode content should work`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Test Set", false)
        val cards = listOf(
            SetCard("card-1", setId, "Карточка", "前面 🌟", "背面 ✨"),
        )

        // Act
        cardSetService.saveCardsForSet(setId, cards)

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertEquals(1, savedCards.size)
        assertEquals("前面 🌟", savedCards[0].frontText)
        assertEquals("背面 ✨", savedCards[0].backText)
    }

    @Test
    fun `getAllCardSets should return sets sorted by creation time descending`() {
        // Arrange
        val setId1 = cardSetService.createCardSet("user-1", "First", false)
        Thread.sleep(10)
        val setId2 = cardSetService.createCardSet("user-2", "Second", false)
        Thread.sleep(10)
        val setId3 = cardSetService.createCardSet("user-3", "Third", false)

        // Act
        val sets = cardSetService.getAllCardSets()

        // Assert
        assertEquals(3, sets.size)
        assertEquals(setId3, sets[0].id, "Most recent should be first")
        assertEquals(setId2, sets[1].id)
        assertEquals(setId1, sets[2].id)
    }

    @Test
    fun `saveCardsForSet with many cards should work efficiently`() {
        // Arrange
        val setId = cardSetService.createCardSet("user-1", "Large Set", false)
        val cards = (1..100).map {
            SetCard("card-$it", setId, "Card $it", "Front $it", "Back $it")
        }

        // Act
        cardSetService.saveCardsForSet(setId, cards)

        // Assert
        val savedCards = cardSetService.getCardsBySetId(setId)
        assertEquals(100, savedCards.size)
    }
}
