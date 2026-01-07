package ru.yarsu.web.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.yarsu.db.CardCreateRequest
import ru.yarsu.db.DatabaseService
import ru.yarsu.web.ApiResponse
import ru.yarsu.web.CardResponse
import ru.yarsu.web.CreateCardResponse
import ru.yarsu.web.ErrorResponse
import java.sql.SQLException

fun Route.cardRoutes(dbService: DatabaseService) {
    route("/cards") {
        get {
            handleGetAllCards(dbService)
        }

        post("/create") {
            handleCreateCard(dbService)
        }

        get("/random") {
            handleGetRandomCard(dbService)
        }

        get("/by-author/{authorId}") {
            handleGetCardsByAuthor(dbService)
        }

        get("/{id}") {
            handleGetCardById(dbService)
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetAllCards(dbService: DatabaseService) {
    try {
        val cards = dbService.getAllCards()
        val cardResponses = cards.map { card ->
            CardResponse(card.id, card.authorId, card.content, card.priority)
        }
        call.respond(
            ApiResponse(
                status = "success",
                message = "Cards retrieved successfully",
                data = cardResponses,
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleCreateCard(dbService: DatabaseService) {
    try {
        val parameters = call.request.queryParameters
        val authorId = parameters["authorId"] ?: "00000000-0000-0000-0000-000000000001"
        val content = parameters["content"] ?: "Sample card content"
        val priority = parameters["priority"]?.toInt() ?: 1

        val cardRequest = CardCreateRequest(authorId, content, priority)
        val cardId = dbService.createCard(cardRequest)

        call.respond(
            CreateCardResponse(
                status = "success",
                cardId = cardId,
                authorId = authorId,
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetRandomCard(dbService: DatabaseService) {
    try {
        val randomCard = dbService.getRandomCardByPriority()

        if (randomCard != null) {
            call.respond(
                ApiResponse(
                    status = "success",
                    message = "Random card retrieved",
                    data = CardResponse(
                        randomCard.id,
                        randomCard.authorId,
                        randomCard.content,
                        randomCard.priority,
                    ),
                ),
            )
        } else {
            call.respond(
                ErrorResponse(
                    status = "empty",
                    error = "No cards available in database",
                ),
            )
        }
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetCardsByAuthor(dbService: DatabaseService) {
    try {
        val authorId = call.parameters["authorId"]
            ?: throw IllegalArgumentException("Author ID is required")

        val cards = dbService.getCardsByAuthor(authorId)
        val cardResponses = cards.map { card ->
            CardResponse(card.id, card.authorId, card.content, card.priority)
        }

        call.respond(
            ApiResponse(
                status = "success",
                message = "Cards retrieved for author $authorId",
                data = cardResponses,
            ),
        )
    } catch (e: IllegalArgumentException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = e.message ?: "Invalid argument",
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetCardById(dbService: DatabaseService) {
    try {
        val id = call.parameters["id"]
            ?: throw IllegalArgumentException("Card ID is required")

        val card = dbService.getCardById(id)

        if (card != null) {
            call.respond(
                ApiResponse(
                    status = "success",
                    message = "Card found",
                    data = CardResponse(card.id, card.authorId, card.content, card.priority),
                ),
            )
        } else {
            call.respond(
                ErrorResponse(
                    status = "not_found",
                    error = "Card with ID $id not found",
                ),
            )
        }
    } catch (e: IllegalArgumentException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = e.message ?: "Invalid argument",
            ),
        )
    } catch (e: SQLException) {
        call.respond(
            ErrorResponse(
                status = "error",
                error = "Database error: ${e.message}",
            ),
        )
    }
}
