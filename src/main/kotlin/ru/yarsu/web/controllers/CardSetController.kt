package ru.yarsu.web.controllers

import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.server.application.call
import io.ktor.server.pebble.PebbleContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.yarsu.models.Card
import ru.yarsu.models.CardSet
import ru.yarsu.services.CardSetService
import ru.yarsu.services.PaginatedResult
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
            val onlyMySets = call.request.queryParameters["only_my"]?.toBoolean() ?: false

            // Определяем userId для фильтрации: если только мои - передаем userId, иначе null для всех видимых
            val filterUserId = if (onlyMySets) session.userId else null

            val result = if (!searchQuery.isNullOrBlank()) {
                if (onlyMySets) {
                    // Если только мои - фильтруем по userId после поиска
                    val allResults = cardSetService.searchSetsPaginatedVisibleToUser(searchQuery, page, DEFAULT_PER_PAGE, session.userId)
                    val myResults = allResults.items.filter { it.userId == session.userId }
                    PaginatedResult(
                        items = myResults,
                        totalItems = myResults.size,
                        currentPage = allResults.currentPage,
                        totalPages = if (myResults.isEmpty()) 1 else allResults.totalPages,
                        perPage = allResults.perPage,
                    )
                } else {
                    cardSetService.searchSetsPaginatedVisibleToUser(searchQuery, page, DEFAULT_PER_PAGE, session.userId)
                }
            } else {
                if (onlyMySets) {
                    // Если только мои - фильтруем по userId
                    val allResults = cardSetService.getSetsPaginatedVisibleToUser(page, DEFAULT_PER_PAGE, session.userId)
                    val myResults = allResults.items.filter { it.userId == session.userId }
                    PaginatedResult(
                        items = myResults,
                        totalItems = myResults.size,
                        currentPage = allResults.currentPage,
                        totalPages = if (myResults.isEmpty()) 1 else allResults.totalPages,
                        perPage = allResults.perPage,
                    )
                } else {
                    cardSetService.getSetsPaginatedVisibleToUser(page, DEFAULT_PER_PAGE, session.userId)
                }
            }

            val model = mapOf<String, Any>(
                "set_of_cards" to result.items,
                "total_sets" to result.totalItems,
                "current_page" to result.currentPage,
                "total_pages" to result.totalPages,
                "per_page" to result.perPage,
                "search_query" to (searchQuery ?: ""),
                "only_my_sets" to onlyMySets,
                "username" to session.username,
                "current_user_id" to session.userId,
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

            // Проверяем, это новый набор или редактирование существующего
            val setId = call.request.queryParameters["id"]
            val newTitle = call.request.queryParameters["new_title"]
            val newIsPrivate = call.request.queryParameters["new_is_private"]?.toBoolean() ?: false

            if (setId.isNullOrBlank() && newTitle.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора или параметры нового набора не указаны")
                return@get
            }

            val model = if (!setId.isNullOrBlank()) {
                // Редактирование существующего набора
                val cardSet = cardSetService.getSetById(setId)
                if (cardSet == null) {
                    call.respond(HttpStatusCode.NotFound, "Набор не найден")
                    return@get
                }

                mapOf<String, Any>(
                    "set_id" to cardSet.id,
                    "set_title" to cardSet.title,
                    "cards" to cardSet.content,
                    "username" to session.username,
                    "is_existing" to true,
                )
            } else {
                // Новый набор - параметры передаются через query params
                mapOf<String, Any>(
                    "set_title" to newTitle!!,
                    "new_is_private" to newIsPrivate,
                    "username" to session.username,
                    "is_existing" to false,
                )
            }

            call.respond(PebbleContent("sets/config-set.html", model))
        }

        route.get("/sets/edit/{setId}") {
            val session = call.requireAuth() ?: return@get

            val setId = call.parameters["setId"]
            if (setId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора не указан")
                return@get
            }

            val cardSet = cardSetService.getSetById(setId)
            if (cardSet == null) {
                call.respond(HttpStatusCode.NotFound, "Набор не найден")
                return@get
            }

            // Проверяем, что пользователь является владельцем набора
            if (cardSet.userId != session.userId) {
                call.respond(HttpStatusCode.Forbidden, "У вас нет прав на редактирование этого набора")
                return@get
            }

            val model = mapOf<String, Any>(
                "set_id" to cardSet.id,
                "title" to cardSet.title,
                "is_private" to cardSet.isPrivate,
                "username" to session.username,
            )

            call.respond(PebbleContent("sets/edit-set.html", model))
        }

        route.post("/sets/edit/{setId}") {
            val session = call.requireAuth() ?: return@post

            val setId = call.parameters["setId"]
            if (setId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора не указан")
                return@post
            }

            val cardSet = cardSetService.getSetById(setId)
            if (cardSet == null) {
                call.respond(HttpStatusCode.NotFound, "Набор не найден")
                return@post
            }

            // Проверяем, что пользователь является владельцем набора
            if (cardSet.userId != session.userId) {
                call.respond(HttpStatusCode.Forbidden, "У вас нет прав на редактирование этого набора")
                return@post
            }

            val parameters = call.receiveParameters()
            val title = parameters["title"] ?: ""
            val isPrivate = parameters["is_private"] != null

            // Валидация названия
            if (title.isBlank()) {
                val model = mapOf<String, Any>(
                    "error" to "Название набора не может быть пустым",
                    "set_id" to setId,
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/edit-set.html", model))
                return@post
            }

            if (title.length > 100) {
                val model = mapOf<String, Any>(
                    "error" to "Название набора не может превышать 100 символов",
                    "set_id" to setId,
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/edit-set.html", model))
                return@post
            }

            // Обновляем только название и приватность
            val updatedSet = cardSet.copy(
                title = title,
                isPrivate = isPrivate,
            )

            val result = cardSetService.updateSet(updatedSet)

            result.onSuccess {
                // Перенаправляем на форму редактирования карточек
                call.respondRedirect("/sets/config?id=$setId")
            }.onFailure { error ->
                val model = mapOf<String, Any>(
                    "error" to (error.message ?: "Произошла ошибка при обновлении набора"),
                    "set_id" to setId,
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/edit-set.html", model))
            }
        }

        route.post("/create-set") {
            val session = call.requireAuth() ?: return@post

            val parameters = call.receiveParameters()
            val title = parameters["title"] ?: ""
            val isPrivate = parameters["is_private"] != null

            // Валидация названия
            if (title.isBlank()) {
                val model = mapOf<String, Any>(
                    "error" to "Название набора не может быть пустым",
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/new-set.html", model))
                return@post
            }

            if (title.length > 100) {
                val model = mapOf<String, Any>(
                    "error" to "Название набора не может превышать 100 символов",
                    "title" to title,
                    "is_private" to isPrivate,
                    "username" to session.username,
                )
                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/new-set.html", model))
                return@post
            }

            // Перенаправляем на форму заполнения БЕЗ сохранения в базу
            val encodedTitle = title.encodeURLParameter()
            call.respondRedirect("/sets/config?new_title=$encodedTitle&new_is_private=$isPrivate")
        }

        route.post("/sets/save") {
            val session = call.requireAuth() ?: return@post
            val parameters = call.receiveParameters()
            val setId = parameters["set_id"] ?: ""
            val newTitle = parameters["new_title"] ?: ""
            val newIsPrivate = parameters["new_is_private"]?.toBoolean() ?: false

            // Собираем карточки из параметров
            val cards = mutableListOf<Card>()
            var index = 0

            while (parameters.contains("cards[$index].question") || parameters.contains("cards[$index].answer")) {
                val title = parameters["cards[$index].title"] ?: ""
                val frontText = parameters["cards[$index].question"] ?: ""
                val backText = parameters["cards[$index].answer"] ?: ""

                if (frontText.isNotBlank() || backText.isNotBlank()) {
                    cards.add(
                        Card(
                            setId = setId.ifBlank { "" }, // Временный ID для новых наборов
                            title = title.ifBlank { null },
                            frontText = frontText,
                            backText = backText,
                        ),
                    )
                }
                index++
            }

            // Проверка, что набор не пустой
            if (cards.isEmpty()) {
                val model = mapOf<String, Any>(
                    "error" to "Невозможно сохранить пустой набор. Добавьте хотя бы одну карточку.",
                    "set_title" to (if (setId.isBlank()) newTitle else ""),
                    "new_is_private" to newIsPrivate,
                    "cards" to cards,
                    "username" to session.username,
                    "is_existing" to setId.isNotBlank(),
                )

                if (setId.isNotBlank()) {
                    model + ("set_id" to setId)
                }

                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/config-set.html", model))
                return@post
            }

            // Определяем, это новый набор или обновление существующего
            val result = if (setId.isBlank()) {
                // Создаём новый набор вместе с карточками
                val newCardSet = CardSet(
                    userId = session.userId,
                    title = newTitle,
                    isPrivate = newIsPrivate,
                    content = cards,
                )
                cardSetService.createSet(newCardSet)
            } else {
                // Обновляем существующий набор
                val existingSet = cardSetService.getSetById(setId)
                if (existingSet == null) {
                    call.respond(HttpStatusCode.NotFound, "Набор не найден")
                    return@post
                }

                val updatedSet = existingSet.copy(content = cards)
                cardSetService.updateSet(updatedSet)
            }

            result.onSuccess {
                call.respondRedirect("/sets")
            }.onFailure { error ->
                val model = mapOf<String, Any>(
                    "error" to (error.message ?: "Произошла ошибка при сохранении набора"),
                    "set_title" to (if (setId.isBlank()) newTitle else ""),
                    "new_is_private" to newIsPrivate,
                    "cards" to cards,
                    "username" to session.username,
                    "is_existing" to setId.isNotBlank(),
                )

                if (setId.isNotBlank()) {
                    model + ("set_id" to setId)
                }

                call.respond(HttpStatusCode.BadRequest, PebbleContent("sets/config-set.html", model))
            }
        }

        route.get("/study/{setId}") {
            val session = call.requireAuth() ?: return@get

            val setId = call.parameters["setId"]
            if (setId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "ID набора не указан")
                return@get
            }

            val cardSet = cardSetService.getSetById(setId)
            if (cardSet == null) {
                call.respond(HttpStatusCode.NotFound, "Набор не найден")
                return@get
            }

            // Проверяем доступ к набору
            if (cardSet.isPrivate && cardSet.userId != session.userId) {
                call.respond(HttpStatusCode.Forbidden, "У вас нет доступа к этому набору")
                return@get
            }

            // Преобразуем карточки в JSON для JavaScript
            val cardsJson = Json.encodeToString(cardSet.content)

            val model = mapOf<String, Any>(
                "set_title" to cardSet.title,
                "cards" to cardSet.content,
                "cards_json" to cardsJson,
                "username" to session.username,
            )

            call.respond(PebbleContent("sets/study.html", model))
        }
    }
}
