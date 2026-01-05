package ru.yarsu.services

import kotlin.math.ceil
import kotlin.math.min
import ru.yarsu.db.DatabaseService
import ru.yarsu.models.CardSet

@Suppress("MagicNumber")
class TestCardSetService(private val databaseService: DatabaseService) : CardSetService {

    private val allSets = fetchCardSetsFromDatabase("12345")

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
                description = dbCardSet.description,
                content = emptyList(),
            )
        }
    }
}