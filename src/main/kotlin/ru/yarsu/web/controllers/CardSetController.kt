package ru.yarsu.web.controllers

import io.ktor.server.pebble.PebbleContent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ru.yarsu.services.CardSetService

@Suppress("MagicNumber")
class CardSetController(private val cardSetService: CardSetService) {

    companion object {
        const val DEFAULT_PER_PAGE = 16
    }

    fun configureRoutes(route: Route) {
        route.get("/sets") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val searchQuery = call.request.queryParameters["search"]

            val result = if (!searchQuery.isNullOrBlank()) {
                cardSetService.searchSetsPaginated(searchQuery, page, DEFAULT_PER_PAGE)
            } else {
                cardSetService.getSetsPaginated(page, DEFAULT_PER_PAGE)
            }

            val model = mapOf<String, Any>(
                "set_of_cards" to result.items,
                "total_sets" to result.totalItems,
                "current_page" to result.currentPage,
                "total_pages" to result.totalPages,
                "per_page" to result.perPage,
                "search_query" to (searchQuery ?: ""),
            )

            call.respond(PebbleContent("sets/sets.html", model))
        }
    }
}
