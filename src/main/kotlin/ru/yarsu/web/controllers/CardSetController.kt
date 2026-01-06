package ru.yarsu.web.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import ru.yarsu.models.Card
import ru.yarsu.models.CardSet
import ru.yarsu.services.CardSetService
import ru.yarsu.web.requireAuth

@Suppress("MagicNumber")
class CardSetController(private val cardSetService: CardSetService) {

    companion object {
        const val DEFAULT_PER_PAGE = 16
    }

    fun configureRoutes(route: Route) {
        route.get("/sets") {
            val session = call.requireAuth() ?: return@get
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
                "username" to session.username,
            )

            call.respond(PebbleContent("sets/sets.html", model))
        }

        route.get("/sets/create") {
            val session = call.requireAuth() ?: return@get
            val model = mapOf<String, Any>(
                "username" to session.username,
            )

            call.respond(PebbleContent("sets/new-set.html", model))
        }

        route.get("/sets/config") {
            val session = call.requireAuth() ?: return@get
            val setId = call.request.queryParameters["id"]

            if (setId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора не указан")
                return@get
            }

            val cardSet = cardSetService.getSetById(setId)
            if (cardSet == null) {
                call.respond(HttpStatusCode.NotFound, "Набор не найден")
                return@get
            }

            val model = mapOf<String, Any>(
                "set_id" to cardSet.id,
                "set_title" to cardSet.title,
                "cards" to cardSet.content,
                "username" to session.username,
            )

            call.respond(PebbleContent("sets/config-set.html", model))
        }

        route.post("/create-set") {
            val session = call.requireAuth() ?: return@post

            val parameters = call.receiveParameters()
            val title = parameters["title"] ?: ""
            val isPrivate = parameters["is_private"] != null

            val cardSet = CardSet(
                userId = session.userId,
                title = title,
                description = null,
                content = emptyList(),
            )

            val result = cardSetService.createSet(cardSet)

            result.onSuccess { createdSet ->
                call.respondRedirect("/sets/config?id=${createdSet.id}")
            }.onFailure { error ->
                val model = mapOf<String, Any>(
                    "error" to (error.message ?: "Произошла ошибка при создании набора"),
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/new-set.html", model))
            }
        }

        route.post("/sets/save") {
            val session = call.requireAuth() ?: return@post
            val parameters = call.receiveParameters()
            val setId = parameters["set_id"] ?: ""

            if (setId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора не указан")
                return@post
            }

            val existingSet = cardSetService.getSetById(setId)
            if (existingSet == null) {
                call.respond(HttpStatusCode.NotFound, "Набор не найден")
                return@post
            }

            // Собираем карточки из параметров
            val cards = mutableListOf<Card>()
            var index = 0

            while (parameters.contains("cards[$index].question") || parameters.contains("cards[$index].answer")) {
                val frontText = parameters["cards[$index].question"] ?: ""
                val backText = parameters["cards[$index].answer"] ?: ""

                if (frontText.isNotBlank() || backText.isNotBlank()) {
                    cards.add(
                        Card(
                            setId = setId,
                            frontText = frontText,
                            backText = backText,
                        ),
                    )
                }
                index++
            }

            // Обновляем набор с новыми карточками
            val updatedSet = existingSet.copy(content = cards)
            val result = cardSetService.updateSet(updatedSet)

            result.onSuccess {
                call.respondRedirect("/sets")
            }.onFailure { error ->
                val model = mapOf<String, Any>(
                    "error" to (error.message ?: "Произошла ошибка при сохранении набора"),
                    "set_id" to setId,
                    "set_title" to existingSet.title,
                    "cards" to cards,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/config-set.html", model))
            }
        }
    }
}
