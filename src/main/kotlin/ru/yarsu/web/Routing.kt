package ru.yarsu.web

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import ru.yarsu.db.CardCreateRequest
import ru.yarsu.db.DatabaseFactory
import ru.yarsu.getDatabaseService

@Serializable
data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null,
)

@Serializable
data class ErrorResponse(
    val status: String,
    val error: String,
)

@Serializable
data class UserResponse(val id: Int, val login: String)

@Serializable
data class CardResponse(val id: Int, val authorId: Int, val content: String, val priority: Int)

@Serializable
data class CardSetResponse(val id: String, val userId: String, val title: String, val description: String)

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
    val timestamp: Long,
)

@Serializable
data class CreateUserResponse(val status: String, val userId: Int, val login: String)

@Serializable
data class CreateCardResponse(val status: String, val cardId: Int, val authorId: Int)

@Serializable
data class CreateCardSetResponse(val status: String, val setId: String, val title: String)

fun Application.configureRouting() {
    val dbService = getDatabaseService()

    routing {
        get("/") {
            call.respondText(
                """
                <html>
                <head><title>FlipSpace API</title></head>
                <body>
                    <h1>FlipSpace API</h1>
                    <h2>Database Endpoints:</h2>
                    <ul>
                        <li><a href="/health">/health</a> - Проверка базы данных</li>
                        <li><a href="/users">/users</a> - Все пользователи</li>
                        <li><a href="/cards">/cards</a> - Все карточки</li>
                        <li><a href="/cards/random">/cards/random</a> - Случайная карточка</li>
                        <li><a href="/sets">/sets</a> - Наборы карточек текущего пользователя</li>
                        <li><a href="/sets/by-user/12345">/sets/by-user/12345</a> - Наборы конкретного пользователя</li>
                        <li><a href="/json/kotlinx-serialization">/json/kotlinx-serialization</a> - Тест сериализации</li>
                    </ul>
                </body>
                </html>
            """.trimIndent(),
            )
        }

        get("/health") {
            try {
                val connection = DatabaseFactory.getConnection()
                val statement = connection.createStatement()
                statement.execute("SELECT 1")
                connection.close()

                call.respond(
                    HealthResponse(
                        status = "healthy",
                        database = "connected",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            } catch (e: Exception) {
                call.respond(
                    HealthResponse(
                        status = "unhealthy",
                        database = "disconnected",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
            }
        }

        route("/users") {
            get {
                try {
                    val users = dbService.getAllUsers()
                    val userResponses = users.map { user ->
                        UserResponse(user.id, user.login)
                    }
                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "Users retrieved successfully",
                            data = userResponses,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get users: ${e.message}",
                        ),
                    )
                }
            }

            post("/create") {
                try {
                    val parameters = call.request.queryParameters
                    val login = parameters["login"] ?: "default_user"
                    val password = parameters["password"] ?: "default_password"

                    val userId = dbService.createUser(login, password)
                    call.respond(
                        CreateUserResponse(
                            status = "success",
                            userId = userId,
                            login = login,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to create user: ${e.message}",
                        ),
                    )
                }
            }

            get("/by-login/{login}") {
                try {
                    val login = call.parameters["login"]
                        ?: throw IllegalArgumentException("Login parameter is required")

                    val user = dbService.getUserByLogin(login)

                    if (user != null) {
                        call.respond(
                            ApiResponse(
                                status = "success",
                                message = "User found",
                                data = UserResponse(user.id, user.login),
                            ),
                        )
                    } else {
                        call.respond(
                            ErrorResponse(
                                status = "not_found",
                                error = "User with login '$login' not found",
                            ),
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get user: ${e.message}",
                        ),
                    )
                }
            }
        }

        route("/cards") {
            get {
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
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get cards: ${e.message}",
                        ),
                    )
                }
            }

            post("/create") {
                try {
                    val parameters = call.request.queryParameters
                    val authorId = parameters["authorId"]?.toInt() ?: 1
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
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to create card: ${e.message}",
                        ),
                    )
                }
            }

            get("/random") {
                try {
                    val randomCard = dbService.getRandomCardByPriority()

                    if (randomCard != null) {
                        call.respond(
                            ApiResponse(
                                status = "success",
                                message = "Random card retrieved",
                                data = CardResponse(randomCard.id, randomCard.authorId, randomCard.content, randomCard.priority),
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
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get random card: ${e.message}",
                        ),
                    )
                }
            }

            get("/by-author/{authorId}") {
                try {
                    val authorId = call.parameters["authorId"]?.toInt()
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
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get cards by author: ${e.message}",
                        ),
                    )
                }
            }

            get("/{id}") {
                try {
                    val id = call.parameters["id"]?.toInt()
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
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get card: ${e.message}",
                        ),
                    )
                }
            }
        }

        route("/sets") {
            get {
                try {
                    val userId = "12345"
                    val cardSets = dbService.getCardSetsByUser(userId)
                    val cardSetResponses = cardSets.map { cardSet ->
                        CardSetResponse(cardSet.id, cardSet.userId, cardSet.title, cardSet.description)
                    }

                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "Card sets retrieved for user $userId",
                            data = cardSetResponses,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get card sets: ${e.message}",
                        ),
                    )
                }
            }

            post("/create") {
                try {
                    val parameters = call.request.queryParameters
                    val userId = parameters["userId"] ?: "12345"
                    val title = parameters["title"] ?: "New Card Set"
                    val description = parameters["description"] ?: ""

                    val setId = dbService.createCardSet(userId, title, description)

                    call.respond(
                        CreateCardSetResponse(
                            status = "success",
                            setId = setId,
                            title = title,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to create card set: ${e.message}",
                        ),
                    )
                }
            }

            get("/by-user/{userId}") {
                try {
                    val userId = call.parameters["userId"]
                        ?: throw IllegalArgumentException("User ID is required")

                    val cardSets = dbService.getCardSetsByUser(userId)
                    val cardSetResponses = cardSets.map { cardSet ->
                        CardSetResponse(cardSet.id, cardSet.userId, cardSet.title, cardSet.description)
                    }

                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "Card sets for user $userId",
                            data = cardSetResponses,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get card sets: ${e.message}",
                        ),
                    )
                }
            }

            get("/all") {
                try {
                    val cardSets = dbService.getAllCardSets()
                    val cardSetResponses = cardSets.map { cardSet ->
                        CardSetResponse(cardSet.id, cardSet.userId, cardSet.title, cardSet.description)
                    }

                    call.respond(
                        ApiResponse(
                            status = "success",
                            message = "All card sets",
                            data = cardSetResponses,
                        ),
                    )
                } catch (e: Exception) {
                    call.respond(
                        ErrorResponse(
                            status = "error",
                            error = "Failed to get all card sets: ${e.message}",
                        ),
                    )
                }
            }
        }

        staticResources("/static", "static")
    }
}