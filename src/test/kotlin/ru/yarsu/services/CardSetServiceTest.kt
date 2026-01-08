package ru.yarsu.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import ru.yarsu.db.CardSetDatabaseService
import ru.yarsu.db.DatabaseService
import ru.yarsu.models.Card
import ru.yarsu.models.CardSet
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardSetServiceTest {

    private lateinit var databaseService: DatabaseService
    private lateinit var cardSetDatabaseService: CardSetDatabaseService
    private lateinit var cardSetService: TestCardSetService

    @Before
    fun setup() {
        cardSetDatabaseService = mockk(relaxed = true)
        databaseService = mockk(relaxed = true)
        every { databaseService.cardSets } returns cardSetDatabaseService

        // Mock empty database initially
        every { cardSetDatabaseService.getAllCardSets() } returns emptyList()

        cardSetService = TestCardSetService(databaseService)
    }

    // ============================================
    // CREATE SET TESTS
    // ============================================

    @Test
    fun `createSet with valid data should succeed`() {
        val userId = "user123"
        val setId = "set123"
        val cards = listOf(
            Card(setId = "", frontText = "Question 1", backText = "Answer 1"),
            Card(setId = "", frontText = "Question 2", backText = "Answer 2"),
        )
        val cardSet = CardSet(
            userId = userId,
            title = "Test Set",
            isPrivate = false,
            content = cards,
        )

        every { cardSetDatabaseService.createCardSet(userId, "Test Set", false) } returns setId
        every { cardSetDatabaseService.saveCardsForSet(setId, any()) } returns Unit

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isSuccess)
        val createdSet = result.getOrNull()
        assertNotNull(createdSet)
        assertEquals(setId, createdSet.id)
        assertEquals("Test Set", createdSet.title)
        assertEquals(userId, createdSet.userId)
        assertEquals(2, createdSet.content.size)

        verify { cardSetDatabaseService.createCardSet(userId, "Test Set", false) }
        verify { cardSetDatabaseService.saveCardsForSet(setId, any()) }
    }

    @Test
    fun `createSet with blank title should fail`() {
        val cardSet = CardSet(
            userId = "user123",
            title = "",
            isPrivate = false,
            content = emptyList(),
        )

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isFailure)
        assertEquals("Название набора не может быть пустым", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createSet with whitespace-only title should fail`() {
        val cardSet = CardSet(
            userId = "user123",
            title = "   ",
            isPrivate = false,
            content = emptyList(),
        )

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isFailure)
        assertEquals("Название набора не может быть пустым", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createSet with title exceeding 100 characters should fail`() {
        val longTitle = "a".repeat(101)
        val cardSet = CardSet(
            userId = "user123",
            title = longTitle,
            isPrivate = false,
            content = emptyList(),
        )

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isFailure)
        assertEquals("Название набора не может превышать 100 символов", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createSet with exactly 100 characters should succeed`() {
        val title = "a".repeat(100)
        val userId = "user123"
        val setId = "set123"
        val cardSet = CardSet(
            userId = userId,
            title = title,
            isPrivate = false,
            content = emptyList(),
        )

        every { cardSetDatabaseService.createCardSet(userId, title, false) } returns setId

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isSuccess)
        verify { cardSetDatabaseService.createCardSet(userId, title, false) }
    }

    @Test
    fun `createSet with private flag should create private set`() {
        val userId = "user123"
        val setId = "set123"
        val cardSet = CardSet(
            userId = userId,
            title = "Private Set",
            isPrivate = true,
            content = emptyList(),
        )

        every { cardSetDatabaseService.createCardSet(userId, "Private Set", true) } returns setId

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isSuccess)
        val createdSet = result.getOrNull()
        assertNotNull(createdSet)
        assertTrue(createdSet.isPrivate)

        verify { cardSetDatabaseService.createCardSet(userId, "Private Set", true) }
    }

    @Test
    fun `createSet with empty cards should succeed`() {
        val userId = "user123"
        val setId = "set123"
        val cardSet = CardSet(
            userId = userId,
            title = "Empty Set",
            isPrivate = false,
            content = emptyList(),
        )

        every { cardSetDatabaseService.createCardSet(userId, "Empty Set", false) } returns setId

        val result = cardSetService.createSet(cardSet)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { cardSetDatabaseService.saveCardsForSet(any(), any()) }
    }

    // ============================================
    // UPDATE SET TESTS
    // ============================================

    @Test
    fun `updateSet with valid data should succeed`() {
        val userId = "user123"
        val setId = "set123"
        val originalSet = CardSet(
            id = setId,
            userId = userId,
            title = "Original Title",
            isPrivate = false,
            content = emptyList(),
        )

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, userId, "Original Title", false),
        )
        cardSetService = TestCardSetService(databaseService)

        val updatedCards = listOf(
            Card(setId = setId, frontText = "New Question", backText = "New Answer"),
        )
        val updatedSet = originalSet.copy(
            title = "Updated Title",
            isPrivate = true,
            content = updatedCards,
        )

        every { cardSetDatabaseService.updateCardSet(setId, "Updated Title", true) } returns Unit
        every { cardSetDatabaseService.saveCardsForSet(setId, any()) } returns Unit

        val result = cardSetService.updateSet(updatedSet)

        assertTrue(result.isSuccess)
        val resultSet = result.getOrNull()
        assertNotNull(resultSet)
        assertEquals("Updated Title", resultSet.title)
        assertTrue(resultSet.isPrivate)
        assertEquals(1, resultSet.content.size)

        verify { cardSetDatabaseService.updateCardSet(setId, "Updated Title", true) }
        verify { cardSetDatabaseService.saveCardsForSet(setId, any()) }
    }

    @Test
    fun `updateSet with blank title should fail`() {
        val setId = "set123"
        val cardSet = CardSet(
            id = setId,
            userId = "user123",
            title = "",
            isPrivate = false,
            content = emptyList(),
        )

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Original Title", false),
        )
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.updateSet(cardSet)

        assertTrue(result.isFailure)
        assertEquals("Название набора не может быть пустым", result.exceptionOrNull()?.message)
    }

    @Test
    fun `updateSet with title exceeding 100 characters should fail`() {
        val setId = "set123"
        val longTitle = "a".repeat(101)
        val cardSet = CardSet(
            id = setId,
            userId = "user123",
            title = longTitle,
            isPrivate = false,
            content = emptyList(),
        )

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Original Title", false),
        )
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.updateSet(cardSet)

        assertTrue(result.isFailure)
        assertEquals("Название набора не может превышать 100 символов", result.exceptionOrNull()?.message)
    }

    @Test
    fun `updateSet with non-existent ID should fail`() {
        val cardSet = CardSet(
            id = "nonexistent",
            userId = "user123",
            title = "Test",
            isPrivate = false,
            content = emptyList(),
        )

        val result = cardSetService.updateSet(cardSet)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("не найден") == true)
    }

    // ============================================
    // DELETE SET TESTS
    // ============================================

    @Test
    fun `deleteSet with valid ID should succeed`() {
        val setId = "set123"

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Test Set", false),
        )
        cardSetService = TestCardSetService(databaseService)

        every { cardSetDatabaseService.deleteCardSet(setId) } returns Unit

        val result = cardSetService.deleteSet(setId)

        assertTrue(result.isSuccess)
        verify { cardSetDatabaseService.deleteCardSet(setId) }

        // Verify set is removed from memory
        assertNull(cardSetService.getSetById(setId))
    }

    @Test
    fun `deleteSet with non-existent ID should fail`() {
        val result = cardSetService.deleteSet("nonexistent")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("не найден") == true)
    }

    @Test
    fun `deleteSet should cascade delete cards`() {
        val setId = "set123"

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Test Set", false),
        )
        cardSetService = TestCardSetService(databaseService)

        every { cardSetDatabaseService.deleteCardSet(setId) } returns Unit

        val result = cardSetService.deleteSet(setId)

        assertTrue(result.isSuccess)
        // The database CASCADE constraint should handle card deletion
        verify { cardSetDatabaseService.deleteCardSet(setId) }
    }

    // ============================================
    // VISIBILITY AND FILTERING TESTS
    // ============================================

    @Test
    fun `getAllSetsVisibleToUser should return all public sets and user's private sets`() {
        val userId = "user123"
        val publicSet1 = CardSet(id = "set1", userId = "otherUser", title = "Public Set 1", isPrivate = false)
        val publicSet2 = CardSet(id = "set2", userId = "otherUser", title = "Public Set 2", isPrivate = false)
        val privateSetOwned = CardSet(id = "set3", userId = userId, title = "My Private Set", isPrivate = true)
        val privateSetOther = CardSet(id = "set4", userId = "otherUser", title = "Other Private Set", isPrivate = true)

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet("set1", "otherUser", "Public Set 1", false),
            ru.yarsu.db.CardSet("set2", "otherUser", "Public Set 2", false),
            ru.yarsu.db.CardSet("set3", userId, "My Private Set", true),
            ru.yarsu.db.CardSet("set4", "otherUser", "Other Private Set", true),
        )
        cardSetService = TestCardSetService(databaseService)

        val visibleSets = cardSetService.getAllSetsVisibleToUser(userId)

        assertEquals(3, visibleSets.size)
        assertTrue(visibleSets.any { it.id == "set1" })
        assertTrue(visibleSets.any { it.id == "set2" })
        assertTrue(visibleSets.any { it.id == "set3" })
        assertFalse(visibleSets.any { it.id == "set4" })
    }

    @Test
    fun `getAllSetsVisibleToUser with null userId should return only public sets`() {
        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet("set1", "user1", "Public Set 1", false),
            ru.yarsu.db.CardSet("set2", "user2", "Public Set 2", false),
            ru.yarsu.db.CardSet("set3", "user1", "Private Set", true),
        )
        cardSetService = TestCardSetService(databaseService)

        val visibleSets = cardSetService.getAllSetsVisibleToUser(null)

        assertEquals(2, visibleSets.size)
        assertTrue(visibleSets.all { !it.isPrivate })
    }

    @Test
    fun `searchSetsVisibleToUser should filter by title and visibility`() {
        val userId = "user123"

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet("set1", "otherUser", "Kotlin Tutorial", false),
            ru.yarsu.db.CardSet("set2", "otherUser", "Java Tutorial", false),
            ru.yarsu.db.CardSet("set3", userId, "Kotlin Advanced", true),
            ru.yarsu.db.CardSet("set4", "otherUser", "Kotlin Basics", true),
        )
        cardSetService = TestCardSetService(databaseService)

        val results = cardSetService.searchSetsVisibleToUser("Kotlin", userId)

        assertEquals(2, results.size)
        assertTrue(results.any { it.title == "Kotlin Tutorial" })
        assertTrue(results.any { it.title == "Kotlin Advanced" })
        assertFalse(results.any { it.title == "Kotlin Basics" }) // Private set from other user
    }

    @Test
    fun `searchSets should be case-insensitive`() {
        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet("set1", "user1", "JavaScript Basics", false),
            ru.yarsu.db.CardSet("set2", "user1", "JAVASCRIPT Advanced", false),
            ru.yarsu.db.CardSet("set3", "user1", "javascript Pro", false),
        )
        cardSetService = TestCardSetService(databaseService)

        val results = cardSetService.searchSets("javascript")

        assertEquals(3, results.size)
    }

    // ============================================
    // PAGINATION TESTS
    // ============================================

    @Test
    fun `getSetsPaginated should return correct first page`() {
        val sets = (1..20).map { i ->
            ru.yarsu.db.CardSet("set$i", "user1", "Set $i", false)
        }

        every { cardSetDatabaseService.getAllCardSets() } returns sets
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetsPaginated(page = 1, perPage = 16)

        assertEquals(16, result.items.size)
        assertEquals(20, result.totalItems)
        assertEquals(1, result.currentPage)
        assertEquals(2, result.totalPages)
        assertEquals(16, result.perPage)
        assertEquals("Set 1", result.items.first().title)
        assertEquals("Set 16", result.items.last().title)
    }

    @Test
    fun `getSetsPaginated should return correct second page`() {
        val sets = (1..20).map { i ->
            ru.yarsu.db.CardSet("set$i", "user1", "Set $i", false)
        }

        every { cardSetDatabaseService.getAllCardSets() } returns sets
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetsPaginated(page = 2, perPage = 16)

        assertEquals(4, result.items.size) // 20 - 16 = 4 items on page 2
        assertEquals(20, result.totalItems)
        assertEquals(2, result.currentPage)
        assertEquals(2, result.totalPages)
        assertEquals("Set 17", result.items.first().title)
        assertEquals("Set 20", result.items.last().title)
    }

    @Test
    fun `getSetsPaginated with page exceeding total should return last page`() {
        val sets = (1..10).map { i ->
            ru.yarsu.db.CardSet("set$i", "user1", "Set $i", false)
        }

        every { cardSetDatabaseService.getAllCardSets() } returns sets
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetsPaginated(page = 999, perPage = 16)

        assertEquals(10, result.items.size)
        assertEquals(1, result.currentPage) // Coerced to valid page
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `getSetsPaginated with empty database should return empty page`() {
        val result = cardSetService.getSetsPaginated(page = 1, perPage = 16)

        assertEquals(0, result.items.size)
        assertEquals(0, result.totalItems)
        assertEquals(1, result.currentPage)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `searchSetsPaginatedVisibleToUser should combine search, visibility and pagination`() {
        val userId = "user123"
        val sets = (1..30).map { i ->
            ru.yarsu.db.CardSet(
                "set$i",
                if (i % 2 == 0) userId else "otherUser",
                if (i % 3 == 0) "Kotlin Set $i" else "Java Set $i",
                i % 4 == 0, // Every 4th set is private
            )
        }

        every { cardSetDatabaseService.getAllCardSets() } returns sets
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.searchSetsPaginatedVisibleToUser(
            query = "Kotlin",
            page = 1,
            perPage = 5,
            userId = userId,
        )

        assertTrue(result.items.size <= 5)
        assertTrue(result.items.all { it.title.contains("Kotlin", ignoreCase = true) })
        assertTrue(result.items.all { !it.isPrivate || it.userId == userId })
    }

    @Test
    fun `getSetsPaginatedVisibleToUser should filter private sets`() {
        val userId = "user123"

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet("set1", "otherUser", "Public 1", false),
            ru.yarsu.db.CardSet("set2", userId, "My Private", true),
            ru.yarsu.db.CardSet("set3", "otherUser", "Other Private", true),
            ru.yarsu.db.CardSet("set4", "otherUser", "Public 2", false),
        )
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetsPaginatedVisibleToUser(1, 10, userId)

        assertEquals(3, result.items.size)
        assertTrue(result.items.any { it.id == "set1" })
        assertTrue(result.items.any { it.id == "set2" })
        assertTrue(result.items.any { it.id == "set4" })
        assertFalse(result.items.any { it.id == "set3" })
    }

    // ============================================
    // GET BY ID TESTS
    // ============================================

    @Test
    fun `getSetById should return set with loaded cards`() {
        val setId = "set123"
        val cards = listOf(
            ru.yarsu.db.SetCard(
                id = "card1",
                setId = setId,
                title = "Card 1",
                frontText = "Question 1",
                backText = "Answer 1",
            ),
            ru.yarsu.db.SetCard(
                id = "card2",
                setId = setId,
                title = null,
                frontText = "Question 2",
                backText = "Answer 2",
            ),
        )

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Test Set", false),
        )
        every { cardSetDatabaseService.getCardsBySetId(setId) } returns cards
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetById(setId)

        assertNotNull(result)
        assertEquals(setId, result.id)
        assertEquals("Test Set", result.title)
        assertEquals(2, result.content.size)
        assertEquals("Question 1", result.content[0].frontText)
        assertEquals("Answer 1", result.content[0].backText)
    }

    @Test
    fun `getSetById with non-existent ID should return null`() {
        val result = cardSetService.getSetById("nonexistent")

        assertNull(result)
    }

    @Test
    fun `getSetById should return empty content list if no cards exist`() {
        val setId = "set123"

        every { cardSetDatabaseService.getAllCardSets() } returns listOf(
            ru.yarsu.db.CardSet(setId, "user123", "Empty Set", false),
        )
        every { cardSetDatabaseService.getCardsBySetId(setId) } returns emptyList()
        cardSetService = TestCardSetService(databaseService)

        val result = cardSetService.getSetById(setId)

        assertNotNull(result)
        assertEquals(0, result.content.size)
    }
}
