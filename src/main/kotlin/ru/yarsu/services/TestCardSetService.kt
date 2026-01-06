package ru.yarsu.services

import kotlin.math.ceil
import kotlin.math.min
import ru.yarsu.db.DatabaseService
import ru.yarsu.models.CardSet

@Suppress("MagicNumber")
class TestCardSetService(private val databaseService: DatabaseService) : CardSetService {

<<<<<<< add-database-fixed
    private val allSets = fetchCardSetsFromDatabase(3)
=======
    private val allSets = generateTestCardSets(50).toMutableList()
>>>>>>> master

    override fun getAllSets(): List<CardSet> = allSets

    override fun searchSets(query: String): List<CardSet> {
        return allSets.filter { set ->
            set.title.contains(query, ignoreCase = true) ||
                    (set.description?.contains(query, ignoreCase = true) ?: false)
        }
    }

    override fun getSetsPaginated(page: Int, perPage: Int): PaginatedResult<CardSet> {
        return createPaginatedResult(allSets, page, perPage)
    }

    override fun searchSetsPaginated(
        query: String,
        page: Int,
        perPage: Int,
    ): PaginatedResult<CardSet> {
        val filteredSets = searchSets(query)
        return createPaginatedResult(filteredSets, page, perPage)
    }

<<<<<<< add-database-fixed
    private fun createPaginatedResult(
        sets: List<CardSet>,
        page: Int,
        perPage: Int,
    ): PaginatedResult<CardSet> {
=======
    override fun createSet(cardSet: CardSet): Result<CardSet> {
        return try {
            if (cardSet.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Название набора не может быть пустым"))
            }

            if (cardSet.title.length > 100) {
                return Result.failure(IllegalArgumentException("Название набора не может превышать 100 символов"))
            }

            allSets.add(cardSet)
            Result.success(cardSet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSetById(id: String): CardSet? {
        return allSets.find { it.id == id }
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

            allSets[index] = cardSet
            Result.success(cardSet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createPaginatedResult(sets: List<CardSet>, page: Int, perPage: Int): PaginatedResult<CardSet> {
>>>>>>> master
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

    private fun fetchCardSetsFromDatabase(userId: Int): List<CardSet> {
        val dbCardSets = databaseService.getCardSetsByUser(userId)
        return dbCardSets.map { dbCardSet ->
            CardSet(
                id = dbCardSet.id,
                userId = dbCardSet.userId,
                title = dbCardSet.title,
                description = dbCardSet.description,
                content = emptyList(),
            )
        }
    }
}