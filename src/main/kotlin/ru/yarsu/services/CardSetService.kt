package ru.yarsu.services

import ru.yarsu.models.CardSet

interface CardSetService {
    fun getAllSets(): List<CardSet>
    fun getAllSetsVisibleToUser(userId: String?): List<CardSet>
    fun searchSets(query: String): List<CardSet>
    fun searchSetsVisibleToUser(query: String, userId: String?): List<CardSet>
    fun getSetsPaginated(page: Int, perPage: Int): PaginatedResult<CardSet>
    fun getSetsPaginatedVisibleToUser(page: Int, perPage: Int, userId: String?): PaginatedResult<CardSet>
    fun searchSetsPaginated(query: String, page: Int, perPage: Int): PaginatedResult<CardSet>
    fun searchSetsPaginatedVisibleToUser(query: String, page: Int, perPage: Int, userId: String?): PaginatedResult<CardSet>
    fun createSet(cardSet: CardSet): Result<CardSet>
    fun getSetById(id: String): CardSet?
    fun updateSet(cardSet: CardSet): Result<CardSet>
    fun deleteSet(id: String): Result<Unit>
}

data class PaginatedResult<T>(
    val items: List<T>,
    val totalItems: Int,
    val currentPage: Int,
    val totalPages: Int,
    val perPage: Int,
)
