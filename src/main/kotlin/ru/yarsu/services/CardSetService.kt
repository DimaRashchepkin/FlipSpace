package ru.yarsu.services

import ru.yarsu.models.CardSet

interface CardSetService {
    fun getAllSets(): List<CardSet>
    fun searchSets(query: String): List<CardSet>
    fun getSetsPaginated(page: Int, perPage: Int): PaginatedResult<CardSet>
    fun searchSetsPaginated(query: String, page: Int, perPage: Int): PaginatedResult<CardSet>
}

data class PaginatedResult<T>(
    val items: List<T>,
    val totalItems: Int,
    val currentPage: Int,
    val totalPages: Int,
    val perPage: Int,
)
