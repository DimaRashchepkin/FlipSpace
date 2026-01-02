package ru.yarsu.services

import ru.yarsu.models.CardSet
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min

@Suppress("MagicNumber")
class TestCardSetService : CardSetService {

    private val allSets = generateTestCardSets(50)

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

    override fun searchSetsPaginated(query: String, page: Int, perPage: Int): PaginatedResult<CardSet> {
        val filteredSets = searchSets(query)
        return createPaginatedResult(filteredSets, page, perPage)
    }

    private fun createPaginatedResult(sets: List<CardSet>, page: Int, perPage: Int): PaginatedResult<CardSet> {
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

    private fun generateTestCardSets(count: Int): List<CardSet> {
        val titles = listOf(
            "Программирование на Kotlin",
            "Английские неправильные глаголы",
            "История Древнего Рима",
            "Химические элементы",
            "Анатомия человека",
            "Философские термины",
            "Финансовая грамотность",
            "Кулинарные рецепты",
            "География мира",
            "Музыкальная теория",
        )

        val descriptions = listOf(
            "Основные концепции и синтаксис языка Kotlin",
            "100 самых важных неправильных глаголов английского языка",
            "Ключевые события и личности Римской империи",
            "Периодическая таблица и свойства элементов",
            "Строение и функции органов человека",
            "Философские термины и концепции",
            "Основы инвестирования и управления личными финансами",
            "Классические рецепты мировой кухни",
            "Столицы, флаги и достопримечательности стран",
            "Ноты, интервалы, аккорды и ритмы",
        )

        return List(count) { index ->
            val titleIndex = index % titles.size
            CardSet(
                id = UUID.randomUUID().toString(),
                userId = "user_${index % 10 + 1}",
                title = "${titles[titleIndex]} ${index / titles.size + 1}",
                description = descriptions[titleIndex],
                content = emptyList(),
            )
        }
    }
}
