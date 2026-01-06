package ru.yarsu.web.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.yarsu.DefaultValues
import ru.yarsu.db.DatabaseService
import ru.yarsu.web.ApiResponse
import ru.yarsu.web.CardSetResponse
import ru.yarsu.web.CreateCardSetResponse
import ru.yarsu.web.ErrorResponse
import java.sql.SQLException

fun Route.cardSetRoutes(dbService: DatabaseService) {
    route("/sets") {
        get {
            handleGetCardSetsForCurrentUser(dbService)
        }

        post("/create") {
            handleCreateCardSet(dbService)
        }

        get("/by-user/{userId}") {
            handleGetCardSetsByUser(dbService)
        }

        get("/all") {
            handleGetAllCardSets(dbService)
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.handleGetCardSetsForCurrentUser(
    dbService: DatabaseService,
) {
    try {
        val userId = DefaultValues.DEFAULT_USER_ID
        val cardSets = dbService.getCardSetsByUser(userId)
        val cardSetResponses = cardSets.map { cardSet ->
            CardSetResponse(cardSet.id, cardSet.userId, cardSet.title)
        }

        call.respond(
            ApiResponse(
                status = "success",
                message = "Card sets retrieved for user $userId",
                data = cardSetResponses,
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

private suspend fun io.ktor.server.routing.RoutingContext.handleCreateCardSet(dbService: DatabaseService) {
    try {
        val parameters = call.request.queryParameters
        val userId = parameters["userId"] ?: DefaultValues.DEFAULT_USER_ID
        val title = parameters["title"] ?: "New Card Set"
        val isPrivate = parameters["isPrivate"]?.toBoolean() ?: false

        val setId = dbService.cardSets.createCardSet(userId, title, isPrivate)

        call.respond(
            CreateCardSetResponse(
                status = "success",
                setId = setId,
                title = title,
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

private suspend fun io.ktor.server.routing.RoutingContext.handleGetCardSetsByUser(dbService: DatabaseService) {
    try {
        val userId = call.parameters["userId"]
            ?: throw IllegalArgumentException("User ID is required")

        val cardSets = dbService.getCardSetsByUser(userId)
        val cardSetResponses = cardSets.map { cardSet ->
            CardSetResponse(cardSet.id, cardSet.userId, cardSet.title)
        }

        call.respond(
            ApiResponse(
                status = "success",
                message = "Card sets for user $userId",
                data = cardSetResponses,
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

private suspend fun io.ktor.server.routing.RoutingContext.handleGetAllCardSets(dbService: DatabaseService) {
    try {
        val cardSets = dbService.getAllCardSets()
        val cardSetResponses = cardSets.map { cardSet ->
            CardSetResponse(cardSet.id, cardSet.userId, cardSet.title)
        }

        call.respond(
            ApiResponse(
                status = "success",
                message = "All card sets",
                data = cardSetResponses,
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
