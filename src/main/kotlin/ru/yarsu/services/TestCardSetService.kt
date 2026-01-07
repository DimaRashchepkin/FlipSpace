package ru.yarsu.services

import ru.yarsu.db.DatabaseService
import ru.yarsu.models.Card
import ru.yarsu.models.CardSet
import kotlin.math.ceil
import kotlin.math.min

@Suppress("MagicNumber")
class TestCardSetService(private val databaseService: DatabaseService) : CardSetService {

    private val allSets = fetchCardSetsFromDatabase("00000000-0000-0000-0000-000000000003").toMutableList()

    override fun getAllSets(): List<CardSet> = allSets

    override fun getAllSetsVisibleToUser(userId: String?): List<CardSet> {
        return allSets.filter { set ->
            !set.isPrivate || set.userId == userId
        }
    }

    override fun searchSets(query: String): List<CardSet> {
        return allSets.filter { set ->
            set.title.contains(query, ignoreCase = true)
        }
    }

    override fun searchSetsVisibleToUser(query: String, userId: String?): List<CardSet> {
        return allSets.filter { set ->
            (!set.isPrivate || set.userId == userId) &&
                set.title.contains(query, ignoreCase = true)
        }
    }

    override fun getSetsPaginated(page: Int, perPage: Int): PaginatedResult<CardSet> {
        return createPaginatedResult(allSets, page, perPage)
    }

    override fun getSetsPaginatedVisibleToUser(page: Int, perPage: Int, userId: String?): PaginatedResult<CardSet> {
        val visibleSets = getAllSetsVisibleToUser(userId)
        return createPaginatedResult(visibleSets, page, perPage)
    }

    override fun searchSetsPaginated(
        query: String,
        page: Int,
        perPage: Int,
    ): PaginatedResult<CardSet> {
        val filteredSets = searchSets(query)
        return createPaginatedResult(filteredSets, page, perPage)
    }

    override fun searchSetsPaginatedVisibleToUser(
        query: String,
        page: Int,
        perPage: Int,
        userId: String?,
    ): PaginatedResult<CardSet> {
        val filteredSets = searchSetsVisibleToUser(query, userId)
        return createPaginatedResult(filteredSets, page, perPage)
    }

    override fun createSet(cardSet: CardSet): Result<CardSet> {
        return try {
            if (cardSet.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название набора не может быть пустым"))
            }

            if (cardSet.title.length > 100) {
                return Result.failure(IllegalArgumentException("Название набора не может превышать 100 символов"))
            }

            // Сохраняем в базу данных вместе с карточками
            val createdId = databaseService.cardSets.createCardSet(
                cardSet.userId,
                cardSet.title,
                cardSet.isPrivate,
            )

            // Сохраняем карточки
            if (cardSet.content.isNotEmpty()) {
                val dbCards = cardSet.content.map { card ->
                    ru.yarsu.db.SetCard(
                        id = card.id,
                        setId = createdId,
                        title = card.title,
                        frontText = card.frontText,
                        backText = card.backText,
                    )
                }
                databaseService.cardSets.saveCardsForSet(createdId, dbCards)
            }

            val createdSet = cardSet.copy(id = createdId)
            allSets.add(createdSet)
            Result.success(createdSet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSetById(id: String): CardSet? {
        val cardSet = allSets.find { it.id == id } ?: return null

        // Загружаем карточки из базы данных
        val dbCards = databaseService.cardSets.getCardsBySetId(id)
        val cards = dbCards.map { dbCard ->
            Card(
                id = dbCard.id,
                setId = dbCard.setId,
                frontText = dbCard.frontText,
                backText = dbCard.backText,
            )
        }

        return cardSet.copy(content = cards)
    }

    override fun updateSet(cardSet: CardSet): Result<CardSet> {
        return try {
            if (cardSet.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название набора не может быть пустым"))
            }

            if (cardSet.title.length > 100) {
                return Result.failure(IllegalArgumentException("Название набора не может превышать 100 символов"))
            }

            val index = allSets.indexOfFirst { it.id == cardSet.id }
            if (index == -1) {
                return Result.failure(IllegalArgumentException("Набор с ID ${cardSet.id} не найден"))
            }

            // Сохраняем карточки в базу данных
            val dbCards = cardSet.content.map { card ->
                ru.yarsu.db.SetCard(
                    id = card.id,
                    setId = cardSet.id,
                    title = card.title,
                    frontText = card.frontText,
                    backText = card.backText,
                )
            }
            databaseService.cardSets.saveCardsForSet(cardSet.id, dbCards)

            allSets[index] = cardSet
            Result.success(cardSet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createPaginatedResult(
        sets: List<CardSet>,
        page: Int,
        perPage: Int,
    ): PaginatedResult<CardSet> {
        val totalItems = sets.size
        val totalPages = if (totalItems > 0) ceil(totalItems.toDouble() / perPage).toInt() else 1
        val currentPage = page.coerceIn(1, totalPages)

        val startIndex = (currentPage - 1) * perPage
        val endIndex = min(startIndex + perPage, totalItems)

        val items = if (startIndex < totalItems) {
            sets.slice(startIndex until endIndex)
        } else {
            emptyList()
        }

        return PaginatedResult(
            items = items,
            totalItems = totalItems,
            currentPage = currentPage,
            totalPages = totalPages,
            perPage = perPage,
        )
    }

    private fun fetchCardSetsFromDatabase(userId: String): List<CardSet> {
        val dbCardSets = databaseService.getCardSetsByUser(userId)
        return dbCardSets.map { dbCardSet ->
            CardSet(
                id = dbCardSet.id,
                userId = dbCardSet.userId,
                title = dbCardSet.title,
                isPrivate = dbCardSet.isPrivate,
                content = emptyList(),
            )
        }
    }
}
